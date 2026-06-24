package br.commons.framework.persistence.jdbc.pool;

import br.commons.Logger;
import br.commons.RT;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import br.commons.framework.persistence.jdbc.Results;
import br.commons.framework.persistence.jdbc.primitives.JDBCConnection;
import br.commons.tools.Strings;
import br.commons.tools.Threads;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@NullMarked
public final class ConnectionPool {
    private final JDBCProperties properties;
    private final BlockingQueue<PooledConnection> availableConnections;
    private final ConcurrentHashMap<Connection, PooledConnection> activeConnections;
    private final AtomicInteger totalConnections;
    private volatile boolean closed;

    public ConnectionPool(JDBCProperties p) {
        this.properties = p;

        closed = false;
        totalConnections = new AtomicInteger(0);
        activeConnections = new ConcurrentHashMap<>();
        // Ilimitada: nunca pode haver mais conexões devolvidas do que o total criado (<= max),
        // e um offer() recusado em silêncio perderia a conexão sem decrementar o total (starve).
        availableConnections = new LinkedBlockingQueue<>();

        // Initialize minimum pool size
        try {
            initialize();
        } catch (SQLException ex ){
            Logger.error("Failed to initialize connection pool '%s': %s", properties.name(), ex.toString());
            throw new RuntimeException("Failed to initialize connection pool: " + ex.toString());
        }

        Threads.virtual(() -> {
            while (RT.running) {
                Threads.sleep(30_000);

                if (closed) return;
                performMaintenance();
            }
        });

        Logger.info("Connection pool '%s' initialized (min=%d, max=%d)", properties.name(), properties.minPoolSize(), properties.maxPoolSize());
        Logger.debug("%s", Logger.lazy(() -> Objects.toString(properties)));
    }

    private void initialize() throws SQLException {
        for (int i = 0; i < properties.minPoolSize(); i++) {
            availableConnections.offer(createConnection());
        }
    }

    /** Cria uma conexão e contabiliza-a no total. Usado por initialize()/manutenção. */
    private PooledConnection createConnection() throws SQLException {
        val pooled = openConnection();
        totalConnections.incrementAndGet();

        Logger.debug("Created new connection for pool '%s' (total=%d)",
                properties.name(), totalConnections.get());

        return pooled;
    }

    /** Abre uma conexão crua sem mexer no contador (o chamador já reservou o lugar). */
    private PooledConnection openConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Connection pool is closed");
        }

        try {
            // Load JDBC driver
            Class.forName(properties.driver());
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found: " + properties.driver(), e);
        }

        val rawConnection = DriverManager.getConnection(
                properties.url(),
                properties.username(),
                properties.password()
        );

        rawConnection.setAutoCommit(properties.autoCommit());

        return new PooledConnection(rawConnection);
    }

    public Result<JDBCConnection, String> aquire() {
        return aquire(properties.connectionTimeout());
    }

    public Result<JDBCConnection, String> aquire(long timeoutMs) {
        if (closed) {
            return Results.resourceIsClosed(properties.name());
        }

        try {
            val pooled = obtain(timeoutMs);
            if (pooled == null) {
                return Results.poolExhausted(properties.name(), timeoutMs);
            }

            // Validate connection
            if (!isValid(pooled)) {
                closeConnection(pooled);
                return aquire(timeoutMs);
            }

            // Update last access time and mark as active
            pooled.updateLastAccess();
            activeConnections.put(pooled.connection(), pooled);

            Logger.trace("Connection acquired from pool '%s' (active=%d, available=%d)",
                    properties.name(), activeConnections.size(), availableConnections.size());

            return Result.success(new JDBCConnection(new ConnectionWrapper(pooled.connection(), this)));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Results.error(properties.name(), e);
        } catch (SQLException e) {
            return Results.error(properties.name(), e);
        }
    }

    /**
     * Tira uma conexão do pool. Primeiro tenta uma conexão ociosa de imediato; se não houver e
     * ainda houver espaço (&lt; max), cria já uma nova — sem bloquear pelo timeout. Só quando o
     * pool está no máximo é que espera (até {@code timeoutMs}) por uma devolução.
     */
    @Nullable
    private PooledConnection obtain(long timeoutMs) throws InterruptedException, SQLException {
        // 1) Caminho rápido: uma conexão ociosa, se existir.
        val pooled = availableConnections.poll();
        if (pooled != null) return pooled;

        // 2) Cresce de imediato se houver lugar — reserva a vaga via CAS (não excede o máximo).
        while (true) {
            val current = totalConnections.get();
            if (current >= properties.maxPoolSize()) break;
            if (totalConnections.compareAndSet(current, current + 1)) {
                try {
                    return openConnection();
                } catch (SQLException ex) {
                    totalConnections.decrementAndGet(); // desfaz a reserva se a criação falhar
                    throw ex;
                }
            }
        }

        // 3) No máximo: espera por uma conexão devolvida.
        return availableConnections.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    void release(Connection connection) {
        val pooled = activeConnections.remove(connection);
        if (pooled == null) {
            Logger.warn("Attempted to release unknown connection to pool '%s'", properties.name());
            return;
        }

        try {
            // Reset connection state
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(properties.autoCommit());
            }

            pooled.updateLastAccess();
            if (!availableConnections.offer(pooled)) {
                closeConnection(pooled); // fila cheia (não devia ocorrer): fecha e decrementa, nunca perde a contagem
            }

            Logger.trace("Connection released to pool '%s' (active=%d, available=%d)",
                    properties.name(), activeConnections.size(), availableConnections.size());

        } catch (SQLException e) {
            Logger.warn("Error releasing connection to pool '%s': %s", properties.name(), Strings.orEmpty(e.getMessage()));
            closeConnection(pooled);
        }
    }

    private boolean isValid(PooledConnection pooled) {
        try {
            // Check if connection exceeded max lifetime
            if (System.currentTimeMillis() - pooled.getCreatedAt() > properties.maxLifetime()) {
                Logger.debug("Connection exceeded max lifetime in pool '%s'", properties.name());
                return false;
            }

            // Use validation query if provided
            if (properties.validationQuery() != null) {
                // NÃO fechar a conexão do pool: apenas o Statement/ResultSet. O try-with-resources
                // sobre pooled.connection() fechava a conexão física a cada validação, deixando o pool
                // com conexões mortas (mas ainda contadas) → churn e starvation sob concorrência.
                val connection = pooled.connection();
                try (val stmt = connection.createStatement()) {
                    stmt.executeQuery(properties.validationQuery()).close();
                }
                return true;
            }

            // Fallback to isValid method
            return pooled.connection().isValid(1);

        } catch (SQLException e) {
            Logger.debug("Connection validation failed in pool '%s': %s", properties.name(), Strings.orEmpty(e.getMessage()));
            return false;
        }
    }

    private void performMaintenance() {
        try {
            Logger.trace("Performing maintenance on pool '%s'", properties.name());

            val removed = removeIdleConnections();
            if (removed > 0) {
                Logger.debug("Removed %d idle connections from pool '%s'", removed, properties.name());
            }

            ensureMinimumPoolSize();

        } catch (Exception e) {
            Logger.error("Error during pool maintenance for '%s': %s", properties.name(), Strings.orEmpty(e.getMessage()));
        }
    }

    /** Remove conexões ociosas além do idle-timeout, sempre preservando o tamanho mínimo do pool. */
    private int removeIdleConnections() {
        val idleDeadline = System.currentTimeMillis() - properties.idleTimeout();
        val iterator = availableConnections.iterator();
        int removed = 0;

        while (iterator.hasNext()) {
            val pooled = iterator.next();

            // Keep minimum pool size
            if (totalConnections.get() <= properties.minPoolSize()) {
                break;
            }

            if (pooled.getLastAccessedAt() < idleDeadline) {
                iterator.remove();
                closeConnection(pooled);
                removed++;
            }
        }

        return removed;
    }

    /** Recria conexões até alcançar o tamanho mínimo do pool. */
    private void ensureMinimumPoolSize() {
        while (totalConnections.get() < properties.minPoolSize()) {
            try {
                availableConnections.offer(createConnection());
            } catch (SQLException e) {
                Logger.error("Failed to maintain minimum pool size for '%s': %s",
                        properties.name(), Strings.orEmpty(e.getMessage()));
                break;
            }
        }
    }

    private void closeConnection(PooledConnection pooled) {
        try {
            pooled.connection().close();
            totalConnections.decrementAndGet();
            Logger.trace("Closed connection from pool '%s' (total=%d)", properties.name(), totalConnections.get());
        } catch (SQLException e) {
            Logger.warn("Error closing connection in pool '%s': %s", properties.name(), Strings.orEmpty(e.getMessage()));
        }
    }

    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        Logger.info("Closing connection pool '%s'", properties.name());

        // Close all active connections (force)
        for (val pooled : activeConnections.values()) {
            Logger.warn("Forcing close of active connection in pool '%s'", properties.name());
            closeConnection(pooled);
        }
        activeConnections.clear();

        // Close all available connections
        PooledConnection pooled;
        while ((pooled = availableConnections.poll()) != null) {
            closeConnection(pooled);
        }

        Logger.info("Connection pool '%s' closed", properties.name());
    }

    public int getActiveCount() {
        return activeConnections.size();
    }

    public int getAvailableCount() {
        return availableConnections.size();
    }

    public int getTotalCount() {
        return totalConnections.get();
    }

    public JDBCProperties getProperties() {
        return properties;
    }

    public boolean isClosed() {
        return closed;
    }

    @NullMarked
    private static class PooledConnection {
        private final Connection connection;
        private final long createdAt;
        private final AtomicLong lastAccessedAt;

        PooledConnection(Connection connection) {
            this.connection = connection;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessedAt = new AtomicLong(this.createdAt);
        }

        Connection connection() {
            return connection;
        }

        long getCreatedAt() {
            return createdAt;
        }

        long getLastAccessedAt() {
            return lastAccessedAt.get();
        }

        void updateLastAccess() {
            lastAccessedAt.set(System.currentTimeMillis());
        }
    }

}

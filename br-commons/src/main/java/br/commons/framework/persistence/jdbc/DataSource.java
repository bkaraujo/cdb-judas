package br.commons.framework.persistence.jdbc;

import br.commons.Logger;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.pool.ConnectionPool;
import br.commons.framework.persistence.jdbc.primitives.JDBCConnection;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.Getter;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Represents a named JDBC data source with connection pooling capabilities.
 * This class wraps a ConnectionPool and provides simplified access to database connections.
 */
@NullMarked
public class DataSource {

    @Getter
    private final String name;
    private final ConnectionPool pool;
    private volatile boolean closed;

    /** Active transactions */
    static final Map<String, ThreadLocal<JDBCTransaction>> transactions = new ConcurrentHashMap<>();

    /**
     * Transações vivas desta DataSource, de todas as threads. O {@link #transactions} é ThreadLocal —
     * uma thread nunca vê o slot de outra —, por isso o {@link #close()} não teria como denunciar
     * transações órfãs sem este registo paralelo. Entra em {@link #begin(String, long)} e sai em
     * {@link JDBCTransaction#close()} (via {@link #unregister(JDBCTransaction)}).
     */
    private final Set<JDBCTransaction> live = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new DataSource with the specified properties.
     *
     * @param properties Configuration properties for this data source
     * @throws IllegalArgumentException if properties are invalid
     * @throws RuntimeException if pool initialization fails
     */
    public DataSource(JDBCProperties properties) {
        this.name = properties.name();
        this.pool = new ConnectionPool(properties);
        this.closed = false;

        Logger.trace("DataSource '%s' created", name);
    }

    public Result<JDBCTransaction, String> begin() {
        return begin("default");
    }

    public Result<JDBCTransaction, String> begin(String name) {
       return begin(name, pool.properties().connectionTimeout());
    }

    public Result<JDBCTransaction, String> begin(long timeout) {
        return begin("default", timeout);
    }

    public Result<JDBCTransaction, String> begin(String name, long timeout) {
        // Uma transação por nome E por thread: cada thread tem o seu próprio slot no ThreadLocal.
        // Reentrância na MESMA thread devolve a transação em curso (re-obtida/continuada); threads
        // distintas nunca partilham a mesma transação/conexão (corrida do modelo antigo, que capturava
        // uma única instância no withInitial e a servia a todas as threads).
        val holder = transactions.computeIfAbsent(name, n -> new ThreadLocal<>());

        // NÃO remover este ramo de criação: na primeira chamada da thread o slot está vazio
        // (holder.get() == null). Sem ele, begin() devolvia Result.success(null), e readOnly()/
        // mutating() caíam de imediato no Logger.fatal("Transaction is null") → toda operação de BD
        // quebrava. É aqui que se adquire a conexão e se vincula a transação ao slot da thread.
        val existing = holder.get();
        if (existing != null) {
            // Propagação REQUIRED: o bloco aninhado participa da transação em curso. O nível é contado
            // para que só o close() do bloco mais externo devolva a conexão ao pool.
            existing.enter();
            return Result.success(existing);
        }

        Logger.trace("Initializing transaction '%s' with timeout: %d", name, timeout);
        return getConnection(timeout).map(connection -> {
            val transaction = new JDBCTransaction(this, name, connection);
            holder.set(transaction);
            live.add(transaction);
            return transaction;
        });
    }

    /** Retira a transação do registo de vivas. Chamado por {@link JDBCTransaction#close()}. */
    void unregister(JDBCTransaction transaction) {
        live.remove(transaction);
    }

    /**
     * Acquires a connection from the pool using the default timeout.
     *
     * @return Result containing a pooled database connection or error message
     */
    private Result<JDBCConnection, String> getConnection() {
        Logger.trace("Acquiring connection");
        if (closed) return Results.resourceIsClosed(name);
        return pool.aquire();
    }

    /**
     * Acquires a connection from the pool with a custom timeout.
     *
     * @param timeoutMs Maximum time to wait for a connection in milliseconds
     * @return Result containing a pooled database connection or error message
     */
    private Result<JDBCConnection, String> getConnection(long timeoutMs) {
        Logger.trace("Acquiring connection with timeout: %d", timeoutMs);
        if (closed) return Results.resourceIsClosed(name);
        return pool.aquire(timeoutMs);
    }

    private <T> T readOnly(Function<JDBCTransaction, Result<T, String>> work) {
        return switch (begin()) {
            case Result.Failure(var error) -> {
                Logger.fatal(error);
                throw new RuntimeException("Unreachable");
            }
            case Result.Success(var transaction) -> {
                if (transaction == null) {
                    Logger.fatal("Transaction is null");
                    throw new RuntimeException("Unreachable");
                }

                try {
                    val result =  work.apply(transaction);
                    // Só reverte se for o nível mais externo: uma leitura aninhada dentro de uma escrita
                    // não pode descartar o trabalho em curso (nem marcar rollbackOnly — não é falha).
                    transaction.rollback();

                    yield result.get();
                } finally { transaction.close(); }
            }
        };
    }

    private <T> T mutating(Function<JDBCTransaction, Result<T, String>> work) {
        return switch (begin()) {
            case Result.Failure(var error) -> {
                Logger.fatal(error);
                throw new RuntimeException("Unreachable");
            }
            case Result.Success(var transaction) -> {
                if (transaction == null) {
                    Logger.fatal("Transaction is null");
                    throw new RuntimeException("Unreachable");
                }
                try {
                    val result = work.apply(transaction);
                    if (result.isSuccess()) {
                        transaction.commit();
                    } else {
                        // Aninhado: commit()/rollback() são no-op, mas a marca envenena o nível externo,
                        // que reverterá tudo em vez de commitar trabalho parcial.
                        transaction.markRollbackOnly();
                        transaction.rollback();
                    }
                    yield result.get();
                } catch (RuntimeException ex) {
                    transaction.markRollbackOnly();
                    transaction.rollback();
                    Logger.fatal(ex.toString());
                    throw new RuntimeException("Unreachable");
                } finally { transaction.close(); }
            }
        };
    }

    public <T> T query(String query, Function<JDBCResultSet, T> function) {
        return readOnly(transaction -> transaction.query(query, function));
    }

    public <T> T query(String query, List<JDBCParameter> parameters, Function<JDBCResultSet, T> function) {
        return readOnly(transaction -> transaction.query(query, parameters, function));
    }

    public boolean execute(String query) {
        return mutating(transaction -> transaction.execute(query));
    }

    public boolean execute(String sql, JDBCParameter... parameters) {
        return mutating(transaction -> transaction.execute(sql, List.of(parameters)));
    }

    public boolean execute(String sql, List<JDBCParameter> parameters) {
        return mutating(transaction -> transaction.execute(sql, parameters));
    }

    /**
     * Executa {@code work} numa transação com propagação REQUIRED: se já houver transação em curso
     * nesta thread, o bloco participa dela (não commita nem devolve a conexão ao pool); caso contrário
     * abre uma nova e commita no fim, ou reverte se {@code work} devolver {@link Result.Failure}.
     */
    public <T> T transaction(Function<JDBCTransaction, Result<T, String>> work) {
        return mutating(work);
    }

    /**
     * Gets the number of currently active connections.
     *
     * @return Number of active connections
     * @see #getAvailableConnections()
     * @see #getTotalConnections()
     */
    public int getActiveConnections() {
        return pool.activeCount();
    }

    /**
     * Gets the number of available connections in the pool.
     *
     * @return Number of available connections
     * @see #getTotalConnections()
     * @see #getActiveConnections()
     */
    public int getAvailableConnections() {
        return pool.availableCount();
    }

    /**
     * Gets the total number of connections (active + available).
     *
     * @return Total number of connections
     * @see #getActiveConnections()
     * @see #getAvailableConnections()
     */
    public int getTotalConnections() {
        return pool.totalCount();
    }

    /**
     * Gets the configuration properties for this data source.
     *
     * @return The JDBC properties
     */
    public JDBCProperties properties() {
        return pool.properties().clone();
    }

    /**
     * Checks if this data source is closed.
     *
     * @return true if closed, false otherwise
     * @see #isOpened()
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Checks if this data source is open.
     *
     * @return true if open, false otherwise
     * @see #isClosed()
     */
    public boolean isOpened() {
        return !closed;
    }

    /**
     * Closes this data source and releases all pooled connections.
     * After calling this method, no more connections can be acquired.
     * Toda transação ainda aberta (sem commit nem rollback) é denunciada em WARN antes de o pool fechar.
     */
    public void close() {
        Logger.trace("Closing DataSource '%s'", name);
        if (closed) { return; }

        closed = true;
        warnPendingTransactions();
        pool.close();
    }

    /**
     * Denuncia em WARN cada transação que continua aberta no momento do fecho: são trabalhos que nunca
     * decidiram commit/rollback e cujo destino passa a ser o rollback implícito do driver ao fechar a
     * conexão. Serve de rastro para o vazamento (que thread e que bloco a deixou aberta).
     */
    private void warnPendingTransactions() {
        for (val transaction : live) {
            if (!transaction.isPending()) { continue; }

            transaction.rollback();
            Logger.warn("DataSource '%s' closed with transaction '%s' still open (thread '%s', depth %d): rollback-ed",
                    name, transaction.name(), transaction.ownerThread(), transaction.depth());
        }

        live.clear();
    }

    @Override
    public String toString() {
        return "DataSource{name='%s', active=%d, available=%d, total=%d, closed=%s}"
                .formatted(name, getActiveConnections(), getAvailableConnections(),
                        getTotalConnections(), closed);
    }
}

package br.commons.framework.persistence.jdbc;

import br.commons.Logger;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.pool.ConnectionPool;
import br.commons.framework.persistence.jdbc.primitives.JDBCConnection;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.Getter;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
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

        Logger.info("DataSource '%s' created", name);
    }

    public Result<JDBCTransaction, String> begin() {
        return getConnection().map(JDBCTransaction::new);
    }

    /** Leitura: executa work e fecha a transação ao final. */
    private <T> Result<T, String> withReadConnection(Function<JDBCTransaction, Result<T, String>> work) {
        return switch (begin()) {
            case Result.Failure(var error) -> new Result.Failure<>(error);
            case Result.Success(var tx) -> {
                if (tx == null) yield new Result.Failure<>("Transaction is null");
                try { yield work.apply(tx); }
                finally { tx.close(); }
            }
        };
    }

    /** Escrita: commita em sucesso, faz rollback em falha/exceção, fecha a transação. */
    private <T> Result<T, String> withWriteConnection(Function<JDBCTransaction, Result<T, String>> work) {
        return switch (begin()) {
            case Result.Failure(var error) -> new Result.Failure<>(error);
            case Result.Success(var tx) -> {
                if (tx == null) yield new Result.Failure<>("Transaction is null");
                try {
                    val r = work.apply(tx);
                    if (r.isSuccess()) tx.commit(); else tx.rollback();
                    yield r;
                } catch (RuntimeException ex) { tx.rollback(); throw ex; }
                finally { tx.close(); }
            }
        };
    }

    public <T> Result<T, String> query(String query, Function<JDBCResultSet, T> function) {
        return withReadConnection(tx -> tx.query(query, function));
    }

    public Result<Boolean, String> execute(String query) {
        return withWriteConnection(tx -> tx.execute(query));
    }

    public <T> Result<T, String> query(String query, List<JDBCPreparedParameter> parameters, Function<JDBCResultSet, T> function) {
        return withReadConnection(tx -> tx.query(query, parameters, function));
    }

    public Result<Boolean, String> execute(String sql, JDBCPreparedParameter... parameters) {
        return withWriteConnection(tx -> tx.execute(sql, List.of(parameters)));
    }

    public <T> Result<T, String> transaction(Function<JDBCTransaction, Result<T, String>> work) {
        return withWriteConnection(work);
    }

    /**
     * Acquires a connection from the pool using the default timeout.
     *
     * @return Result containing a pooled database connection or error message
     */
    public Result<JDBCConnection, String> getConnection() {
        if (closed) return Results.resourceIsClosed(name);
        return pool.aquire();
    }

    /**
     * Acquires a connection from the pool with a custom timeout.
     *
     * @param timeoutMs Maximum time to wait for a connection in milliseconds
     * @return Result containing a pooled database connection or error message
     */
    public Result<JDBCConnection, String> getConnection(long timeoutMs) {
        if (closed) return Results.resourceIsClosed(name);
        return pool.aquire(timeoutMs);
    }

    /**
     * Gets the number of currently active connections.
     *
     * @return Number of active connections
     */
    public int getActiveConnections() {
        return pool.getActiveCount();
    }

    /**
     * Gets the number of available connections in the pool.
     *
     * @return Number of available connections
     */
    public int getAvailableConnections() {
        return pool.getAvailableCount();
    }

    /**
     * Gets the total number of connections (active + available).
     *
     * @return Total number of connections
     */
    public int getTotalConnections() {
        return pool.getTotalCount();
    }

    /**
     * Gets the configuration properties for this data source.
     *
     * @return The JDBC properties
     */
    public JDBCProperties getProperties() {
        return pool.getProperties();
    }

    /**
     * Checks if this data source is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this data source and releases all pooled connections.
     * After calling this method, no more connections can be acquired.
     */
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        Logger.info("Closing DataSource '%s'", name);
        pool.close();
        Logger.info("DataSource '%s' closed", name);
    }

    @Override
    public String toString() {
        return "DataSource{name='%s', active=%d, available=%d, total=%d, closed=%s}"
                .formatted(name, getActiveConnections(), getAvailableConnections(),
                        getTotalConnections(), closed);
    }
}

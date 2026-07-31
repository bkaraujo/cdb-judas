package br.commons.framework.persistence.jdbc;

import br.commons.Logger;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.primitives.JDBCConnection;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.Function;

@NullMarked
public class JDBCTransaction {

    private final DataSource dataSource;

    private final JDBCConnection connection;

    private final String name;

    /** Thread que abriu a transação — só serve para identificar o dono no aviso de transação órfã. */
    private final String ownerThread;

    /**
     * Nível de reentrância (propagação REQUIRED): a instância nasce no nível 1 (o {@code begin()} que a
     * criou) e cada {@code begin()} reentrante na mesma thread incrementa. Só o nível mais externo
     * commita/reverte e devolve a conexão ao pool — os aninhados apenas participam.
     */
    private int depth = 1;

    /** Marcado por um bloco aninhado que falhou: o nível externo reverte em vez de commitar. */
    private boolean rollbackOnly;

    /**
     * {@code true} depois de o nível mais externo ter commitado ou revertido. Enquanto for {@code false}
     * a transação está aberta e pendente de decisão — é o que {@link DataSource#close()} denuncia.
     */
    private volatile boolean settled;

    JDBCTransaction(DataSource dataSource, String name, JDBCConnection connection) {
        this.dataSource = dataSource;
        this.name = name;
        this.connection = connection;
        this.ownerThread = Thread.currentThread().getName();
    }

    /** Regista mais um nível de reentrância. Chamado por {@link DataSource#begin(String, long)}. */
    void enter() {
        depth++;
        Logger.trace("Transaction depth %d", depth);
    }

    /** {@code true} quando esta é a transação mais externa (a que commita/reverte de facto). */
    boolean isOutermost() {
        return depth == 1;
    }

    /** Envenena a transação: o nível mais externo reverterá mesmo que o seu próprio trabalho tenha êxito. */
    void markRollbackOnly() {
        rollbackOnly = true;
    }

    boolean isRollbackOnly() {
        return rollbackOnly;
    }

    /** {@code true} enquanto a transação estiver aberta sem commit nem rollback. */
    boolean isPending() {
        return !settled;
    }

    String name() {
        return name;
    }

    String ownerThread() {
        return ownerThread;
    }

    int depth() {
        return depth;
    }

    public <T> Result<T, String> execute(Function<JDBCConnection, Result<T, String>> function) {
        try { return function.apply(connection); }
        finally { close(); }
    }

    public Result<Boolean, String> execute(String sql, JDBCParameter... parameters) {
        return execute(sql, List.of(parameters));
    }

    public Result<Boolean, String> execute(String statement, List<JDBCParameter> parameters) {
        return switch (connection.prepareStatement(statement)) {
            case Result.Failure(var error) -> new Result.Failure<>(error);
            case Result.Success(var pstmt) -> {
                if (pstmt == null) yield new Result.Failure<>("PreparedStatement is null");
                try {
                    for (var i = 0; i < parameters.size(); i++) {
                        val parameter = parameters.get(i);
                        pstmt.setObject(i + 1, parameter.value());
                    }

                    yield pstmt.execute();
                } finally { pstmt.close(); }
            }
        };
    }

    public Result<Boolean, String> execute(String statement) {
        return switch (connection.createStatement()) {
            case Result.Failure(var error) -> new Result.Failure<>(error);
            case Result.Success(var pstmt) -> {
                if (pstmt == null) yield new Result.Failure<>("PreparedStatement is null");
                try { yield pstmt.execute(statement); }
                finally { pstmt.close(); }
            }
        };
    }

    public <T> Result<T, String> query(String query, List<JDBCParameter> parameters, Function<JDBCResultSet, T> function) {
        return switch (connection.prepareStatement(query)) {
            case Result.Failure(var error) -> new Result.Failure<>(error);
            case Result.Success(var pstmt) -> {
                if (pstmt == null) yield new Result.Failure<>("PreparedStatement is null");
                try {
                    for (var i = 0; i < parameters.size(); i++) {
                        val parameter = parameters.get(i);
                        pstmt.setObject(i + 1, parameter.value());
                    }
                    yield switch (pstmt.executeQuery()) {
                        case Result.Failure(var error) -> new Result.Failure<>(error);
                        case Result.Success(var rs) -> {
                            if (rs == null) yield new Result.Failure<>("ResultSet is null");
                            try { yield new Result.Success<>(function.apply(rs)); }
                            finally { rs.close(); }
                        }
                    };
                } finally { pstmt.close(); }
            }
        };
    }

    public <T> Result<T, String> query(String query, Function<JDBCResultSet, T> function) {
        return switch (connection.createStatement()) {
            case Result.Failure(var error) -> new Result.Failure<>(error);
            case Result.Success(var stmt) -> {
                if (stmt == null) yield new Result.Failure<>("Statement is null");

                try {
                    yield switch (stmt.executeQuery(query)) {
                        case Result.Failure(var error) -> new Result.Failure<>(error);
                        case Result.Success(var rs) -> {
                            if (rs == null) yield new Result.Failure<>("ResultSet is null");
                            try { yield new Result.Success<>(function.apply(rs)); }
                            finally { rs.close(); }
                        }
                    };
                } finally { stmt.close(); }
            }
        };
    }

    public Result<Boolean, String> commit() {
        // Bloco aninhado não commita: quem decide é o nível mais externo (propagação REQUIRED).
        if (!isOutermost()) return Result.success(true);
        if (rollbackOnly) return rollback();

        Logger.trace("Committing transaction");
        return connection.commit().map(c -> {
            settled = true;
            connection.close() ;
            return true;
        });
    }

    public Result<Boolean, String> rollback() {
        // Bloco aninhado não reverte a conexão: só sinaliza (ver markRollbackOnly) e deixa ao externo.
        if (!isOutermost()) return Result.success(true);

        Logger.trace("Rolling back transaction");
        return connection.rollback().map(c -> {
            settled = true;
            connection.close() ;
            return true;
        });
    }

    /**
     * Fecha um nível de reentrância. Só o último ({@code depth} a chegar a zero) devolve a conexão ao
     * pool e liberta o slot da thread — sem isto, um {@code close()} aninhado (ex.: o {@code finally} de
     * {@link #execute(Function)}, usado na introspecção dos repositórios) encerrava a transação alheia.
     */
    public void close() {
        if (--depth > 0) {
            Logger.trace("Transaction depth %d", depth);
            return;
        }

        connection.close()
                .ifSuccess(c -> unbind())
                .ifFailure(error -> Logger.error("Error closing JDBC transaction: %s", error));
    }

    /** Liberta o slot da transação nesta thread para que o próximo begin() crie uma nova. */
    private void unbind() {
        dataSource.unregister(this);

        val holder = DataSource.transactions.get(name);
        if (holder != null) {
            holder.remove();
        }
    }

}

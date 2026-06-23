package br.commons.framework.persistence.jdbc;

import br.commons.Logger;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.primitives.JDBCConnection;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.Function;

@NullMarked
public class JDBCTransaction {

    private final JDBCConnection connection;

    JDBCTransaction(JDBCConnection connection) {
        this.connection = connection;
    }

    public <T> Result<T, String> execute(Function<JDBCConnection, Result<T, String>> function) {
        try { return function.apply(connection); }
        finally { close(); }
    }

    public Result<Boolean, String> execute(String sql, JDBCPreparedParameter... parameters) {
        return execute(sql, List.of(parameters));
    }

    public Result<Boolean, String> execute(String statement, List<JDBCPreparedParameter> parameters) {
        return switch (connection.prepareStatement(statement)) {
            case Result.Failure(var error) -> new Result.Failure<>(error);
            case Result.Success(var pstmt) -> {
                if (pstmt == null) yield new Result.Failure<>("PreparedStatement is null");
                try {

                    Logger.trace(statement);
                    for (var i = 0; i < parameters.size(); i++) {
                        val parameter = parameters.get(i);
                        Logger.verbose(" %d = %s", i + 1, Strings.or(parameter.value(), "null"));
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
                try {

                    Logger.trace(statement);
                    yield pstmt.execute(statement);
                } finally { pstmt.close(); }
            }
        };
    }

    public <T> Result<T, String> query(String query, List<JDBCPreparedParameter> parameters, Function<JDBCResultSet, T> function) {
        return switch (connection.prepareStatement(query)) {
            case Result.Failure(var error) -> new Result.Failure<>(error);
            case Result.Success(var pstmt) -> {
                if (pstmt == null) yield new Result.Failure<>("PreparedStatement is null");
                try {
                    Logger.trace(query);
                    for (var i = 0; i < parameters.size(); i++) {
                        val parameter = parameters.get(i);
                        Logger.verbose(" %d = %s", i + 1, Strings.or(parameter.value(), "null"));
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
                    Logger.verbose(query);
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
        return connection.commit().map(c -> {
            connection.close() ;
            return true;
        });
    }

    public Result<Boolean, String> rollback() {
        return connection.rollback().map(c -> {
            connection.close() ;
            return true;
        });
    }

    public void close() {
        connection.close().ifFailure(error -> Logger.error("Error closing JDBC transaction: %s", error));
    }

}

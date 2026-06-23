package br.commons.framework.persistence;

import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCConnection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class JDBCTransaction {

    private final DataSource ds = Registry.get(DataSource.class);
    private @Nullable JDBCConnection connection;

    public Result<Boolean, String> begin() {
        if (connection != null) return Result.success(true);
        return ds.getConnection().map(c -> { connection = c; return true; });
    }

    public Result<Boolean, String> commit() {
        if (connection == null) return Result.failure("Not in active transaction");
        return connection.commit().map(c -> {
            connection.close() ;
            connection = null;
            return true;
        });
    }

    public Result<Boolean, String> rollback() {
        if (connection == null) return Result.failure("Not in active transaction");
        return connection.rollback().map(c -> {
            connection.close() ;
            connection = null;
            return true;
        });
    }

}

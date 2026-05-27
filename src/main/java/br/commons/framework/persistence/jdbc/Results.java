package br.commons.framework.persistence.jdbc;

import br.commons.Result;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class Results {
    private Results(){}

    public static <T> Result<T, String> dataSourceNotFound(String name)  {
        return Result.failure(
                "DataSource '%s' not found".formatted(name)
        );
    }

    public static <T> Result<T, String> resourceIsClosed(String name)  {
        return Result.failure(
                "Resource '%s' is closed".formatted(name)
        );
    }

    public static <T> Result<T, String> noDefaultDatasource()  {
        return Result.failure(
                "No default DataSource configured"
        );
    }

    public static <T> Result<T, String> poolExhausted(String datasource, long timeout)  {
        return Result.failure(
                "Connection timeout: no connection available within %dms for pool '%s'".formatted(timeout, datasource)
        );
    }

    public static <T> Result<T, String> error(String datasource, Throwable cause)  {
        return Result.failure(
                "%s: %s".formatted(datasource, cause)
        );
    }

    public static <T> Result<T, String> error(String text)  {
        return Result.failure(
                text
        );
    }

}

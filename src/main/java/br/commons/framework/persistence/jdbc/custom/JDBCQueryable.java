package br.commons.framework.persistence.jdbc.custom;

import br.commons.Result;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface JDBCQueryable {

    Result<Void, String> close();

    Result<Boolean, String> execute(String sql);
    Result<Boolean, String> execute(String sql, JDBCPreparedParameter ... parameter);

    Result<Integer, String> update(String sql);
    Result<Integer, String> update(String sql, JDBCPreparedParameter ... parameter);

    Result<JDBCResultSet, String> query(String sql);
    Result<JDBCResultSet, String> query(String sql, JDBCPreparedParameter ... parameter);

}

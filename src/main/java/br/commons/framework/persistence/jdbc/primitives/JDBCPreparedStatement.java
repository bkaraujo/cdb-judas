package br.commons.framework.persistence.jdbc.primitives;

import br.commons.Logger;
import br.commons.Result;
import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

@NullMarked
public record JDBCPreparedStatement(
        PreparedStatement delegate
) {

    public Result<JDBCResultSet, String> executeQuery() {
        Logger.trace("executeQuery");
        try { return Result.success(new JDBCResultSet(delegate.executeQuery())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> executeUpdate() {
        Logger.trace("executeUpdate");
        try { return Result.success(delegate.executeUpdate()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNull(int parameterIndex, int sqlType) {
        Logger.verbose("%d = %d", parameterIndex, sqlType);
        try { delegate.setNull(parameterIndex, sqlType); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBoolean(int parameterIndex, boolean x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setBoolean(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setByte(int parameterIndex, byte x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setByte(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setShort(int parameterIndex, short x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setShort(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setInt(int parameterIndex, int x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setInt(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setLong(int parameterIndex, long x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setLong(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setFloat(int parameterIndex, float x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setFloat(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setDouble(int parameterIndex, double x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setDouble(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBigDecimal(int parameterIndex, BigDecimal x) {
        Logger.verbose("%d = %s", parameterIndex, x.toPlainString());
        try { delegate.setBigDecimal(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setString(int parameterIndex, String x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setString(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBytes(int parameterIndex, byte[] x) {
        Logger.verbose("%d = %s", parameterIndex, x.length);
        try { delegate.setBytes(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setDate(int parameterIndex, Date x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setDate(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTime(int parameterIndex, Time x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setTime(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTimestamp(int parameterIndex, Timestamp x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setTimestamp(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setAsciiStream(int parameterIndex, InputStream x, int length) {
        Logger.verbose("%d, length %s", parameterIndex, length);
        try { delegate.setAsciiStream(parameterIndex, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBinaryStream(int parameterIndex, InputStream x, int length) {
        Logger.verbose("%d, length %s", parameterIndex, length);
        try { delegate.setBinaryStream(parameterIndex, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> clearParameters() {
        Logger.trace("clearParameters");
        try { delegate.clearParameters(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setObject(int parameterIndex, Object x, int targetSqlType) {
        Logger.verbose("%d type %s %s", parameterIndex, targetSqlType, x);
        try { delegate.setObject(parameterIndex, x, targetSqlType); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setObject(int parameterIndex, @Nullable Object x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setObject(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> execute() {
        Logger.trace("execute");
        try { return Result.success(delegate.execute()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> addBatch() {
        Logger.trace("addBatch");
        try { delegate.addBatch(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCharacterStream(int parameterIndex, Reader reader, int length) {
        Logger.trace("setCharacterStream");
        try { delegate.setCharacterStream(parameterIndex, reader, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setRef(int parameterIndex, Ref x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setRef(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBlob(int parameterIndex, Blob x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setBlob(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClob(int parameterIndex, Clob x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setClob(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setArray(int parameterIndex, Array x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setArray(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<ResultSetMetaData, String> getMetaData() {
        Logger.trace("getMetaData");
        try { return Result.success(delegate.getMetaData()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setDate(int parameterIndex, Date x, Calendar cal) {
        Logger.verbose("%d = %s %s", parameterIndex, x, cal);
        try { delegate.setDate(parameterIndex, x, cal); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTime(int parameterIndex, Time x, Calendar cal) {
        Logger.verbose("%d = %s %s", parameterIndex, x, cal);
        try { delegate.setTime(parameterIndex, x, cal); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTimestamp(int parameterIndex, Timestamp x, Calendar cal) {
        Logger.verbose("%d = %s %s", parameterIndex, x, cal);
        try { delegate.setTimestamp(parameterIndex, x, cal); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNull(int parameterIndex, int sqlType, String typeName) {
        Logger.verbose("%d = %s %s", parameterIndex, sqlType, typeName);
        try { delegate.setNull(parameterIndex, sqlType, typeName); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setURL(int parameterIndex, URL x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setURL(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<ParameterMetaData, String> getParameterMetaData() {
        Logger.trace("getParameterMetaData");
        try { return Result.success(delegate.getParameterMetaData()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setRowId(int parameterIndex, RowId x) {
        Logger.verbose("%d = %s", parameterIndex, x);
        try { delegate.setRowId(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNString(int parameterIndex, String value) {
        Logger.verbose("%d = %s", parameterIndex, value);
        try { delegate.setNString(parameterIndex, value); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNCharacterStream(int parameterIndex, Reader value, long length) {
        Logger.verbose("%d length %s", parameterIndex, length);
        try { delegate.setNCharacterStream(parameterIndex, value, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNClob(int parameterIndex, NClob value) {
        Logger.verbose("%d = %s", parameterIndex, value);
        try { delegate.setNClob(parameterIndex, value); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClob(int parameterIndex, Reader reader, long length) {
        Logger.verbose("%d length %s", parameterIndex, length);
        try { delegate.setClob(parameterIndex, reader, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBlob(int parameterIndex, InputStream inputStream, long length) {
        Logger.verbose("%d length %s", parameterIndex, length);
        try { delegate.setBlob(parameterIndex, inputStream, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNClob(int parameterIndex, Reader reader, long length) {
        Logger.verbose("%d length %s", parameterIndex, length);
        try { delegate.setNClob(parameterIndex, reader, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setSQLXML(int parameterIndex, SQLXML xmlObject) {
        Logger.verbose("%d = %s", parameterIndex, xmlObject);
        try { delegate.setSQLXML(parameterIndex, xmlObject); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) {
        Logger.verbose("%d length %s", parameterIndex, scaleOrLength);
        try { delegate.setObject(parameterIndex, x, targetSqlType, scaleOrLength); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setAsciiStream(int parameterIndex, InputStream x, long length) {
        Logger.verbose("%d length %s", parameterIndex, length);
        try { delegate.setAsciiStream(parameterIndex, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBinaryStream(int parameterIndex, InputStream x, long length) {
        Logger.verbose("%d length %s", parameterIndex, length);
        try { delegate.setBinaryStream(parameterIndex, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCharacterStream(int parameterIndex, Reader reader, long length) {
        Logger.verbose("%d length %s", parameterIndex, length);
        try { delegate.setCharacterStream(parameterIndex, reader, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setAsciiStream(int parameterIndex, InputStream x) {
        try { delegate.setAsciiStream(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBinaryStream(int parameterIndex, InputStream x) {
        try { delegate.setBinaryStream(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCharacterStream(int parameterIndex, Reader reader) {
        try { delegate.setCharacterStream(parameterIndex, reader); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNCharacterStream(int parameterIndex, Reader value) {
        try { delegate.setNCharacterStream(parameterIndex, value); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClob(int parameterIndex, Reader reader) {
        try { delegate.setClob(parameterIndex, reader); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBlob(int parameterIndex, InputStream inputStream) {
        try { delegate.setBlob(parameterIndex, inputStream); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNClob(int parameterIndex, Reader reader) {
        try { delegate.setNClob(parameterIndex, reader); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> close() {
        Logger.trace("close");
        try { delegate.close(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxFieldSize() {
        Logger.trace("getMaxFieldSize");
        try { return Result.success(delegate.getMaxFieldSize()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setMaxFieldSize(int max) {
        try { delegate.setMaxFieldSize(max); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxRows() {
        Logger.trace("getMaxRows");
        try { return Result.success(delegate.getMaxRows()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setMaxRows(int max) {
        Logger.trace("setMaxRows");
        try { delegate.setMaxRows(max); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setEscapeProcessing(boolean enable) {
        Logger.trace("setEscapeProcessing");
        try { delegate.setEscapeProcessing(enable); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getQueryTimeout() {
        Logger.trace("getQueryTimeout");
        try { return Result.success(delegate.getQueryTimeout()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setQueryTimeout(int seconds) {
        Logger.trace("setQueryTimeout");
        try { delegate.setQueryTimeout(seconds); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> cancel() {
        Logger.trace("cancel");
        try { delegate.cancel(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLWarning, String> getWarnings() {
        Logger.trace("getWarnings");
        try { return Result.success(delegate.getWarnings()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> clearWarnings() {
        Logger.trace("clearWarnings");
        try { delegate.clearWarnings(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCursorName(String name) {
        Logger.trace("setCursorName");
        try { delegate.setCursorName(name); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getResultSet() {
        Logger.trace("getResultSet");
        try { return Result.success(new JDBCResultSet(delegate.getResultSet())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getUpdateCount() {
        Logger.trace("getUpdateCount");
        try { return Result.success(delegate.getUpdateCount()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getMoreResults() {
        Logger.trace("getMoreResults");
        try { return Result.success(delegate.getMoreResults()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setFetchDirection(int direction) {
        Logger.trace("setFetchDirection");
        try { delegate.setFetchDirection(direction); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getFetchDirection() {
        Logger.trace("getFetchDirection");
        try { return Result.success(delegate.getFetchDirection()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setFetchSize(int rows) {
        Logger.trace("setFetchSize");
        try { delegate.setFetchSize(rows); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getFetchSize() {
        Logger.trace("getFetchSize");
        try { return Result.success(delegate.getFetchSize()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getResultSetConcurrency() {
        Logger.trace("getResultSetConcurrency");
        try { return Result.success(delegate.getResultSetConcurrency()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getResultSetType() {
        Logger.trace("getResultSetType");
        try { return Result.success(delegate.getResultSetType()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> addBatch(String sql) {
        Logger.trace("addBatch");
        Logger.verbose(sql);
        try { delegate.addBatch(sql); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> clearBatch() {
        Logger.trace("clearBatch");
        try { delegate.clearBatch(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<int[], String> executeBatch() {
        Logger.trace("executeBatch");
        try { return Result.success(delegate.executeBatch()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCConnection, String> getConnection() {
        Logger.trace("getConnection");
        try { return Result.success(new JDBCConnection(delegate.getConnection())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getMoreResults(int current) {
        Logger.trace("getMoreResults");
        try { return Result.success(delegate.getMoreResults(current)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getGeneratedKeys() {
        Logger.trace("getGeneratedKeys");
        try { return Result.success(new JDBCResultSet(delegate.getGeneratedKeys())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> executeUpdate(String sql, int autoGeneratedKeys) {
        Logger.trace("executeUpdate");
        Logger.verbose(sql);
        try { return Result.success(delegate.executeUpdate(sql, autoGeneratedKeys)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> executeUpdate(String sql, int[] columnIndexes) {
        Logger.trace("executeUpdate");
        Logger.verbose(sql);
        try { return Result.success(delegate.executeUpdate(sql, columnIndexes)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> executeUpdate(String sql, String[] columnNames) {
        Logger.trace("executeUpdate");
        Logger.verbose(sql);
        try { return Result.success(delegate.executeUpdate(sql, columnNames)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> execute(String sql, int autoGeneratedKeys) {
        Logger.trace("execute");
        Logger.verbose(sql);
        try { return Result.success(delegate.execute(sql, autoGeneratedKeys)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> execute(String sql, int[] columnIndexes) {
        Logger.trace("execute");
        Logger.verbose(sql);
        try { return Result.success(delegate.execute(sql, columnIndexes)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> execute(String sql, String[] columnNames) {
        Logger.trace("execute");
        Logger.verbose(sql);
        try { return Result.success(delegate.execute(sql, columnNames)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getResultSetHoldability() {
        Logger.trace("getResultSetHoldability");
        try { return Result.success(delegate.getResultSetHoldability()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isClosed() {
        Logger.trace("isClosed");
        try { return Result.success(delegate.isClosed()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setPoolable(boolean poolable) {
        Logger.trace("setPoolable");
        try { delegate.setPoolable(poolable); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isPoolable() {
        Logger.trace("isPoolable");
        try { return Result.success(delegate.isPoolable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> closeOnCompletion() {
        Logger.trace("closeOnCompletion");
        try { delegate.closeOnCompletion(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isCloseOnCompletion() {
        Logger.trace("isCloseOnCompletion");
        try { return Result.success(delegate.isCloseOnCompletion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public <T> Result<T, String> unwrap(Class<T> iface) {
        Logger.trace("unwrap");
        try { return Result.success(delegate.unwrap(iface)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isWrapperFor(Class<?> iface) {
        Logger.trace("isWrapperFor");
        try { return Result.success(delegate.isWrapperFor(iface)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }
}

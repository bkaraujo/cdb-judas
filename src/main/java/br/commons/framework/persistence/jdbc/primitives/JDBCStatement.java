package br.commons.framework.persistence.jdbc.primitives;

import br.commons.Logger;
import br.commons.Result;
import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

@NullMarked
public record JDBCStatement(
        Statement delegate
) {

    public Result<JDBCResultSet, String> executeQuery(String sql) {
        Logger.trace("executeQuery");
        Logger.verbose(sql);
        try { return Result.success(new JDBCResultSet(delegate.executeQuery(sql))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> executeUpdate(String sql) {
        Logger.trace("executeUpdate");
        Logger.verbose(sql);
        try { return Result.success(delegate.executeUpdate(sql)); }
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
        Logger.trace("setMaxFieldSize");
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

    public Result<Boolean, String> execute(String sql) {
        Logger.trace("execute");
        Logger.verbose(sql);
        try { return Result.success(delegate.execute(sql)); }
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

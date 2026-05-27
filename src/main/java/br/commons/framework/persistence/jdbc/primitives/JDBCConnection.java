package br.commons.framework.persistence.jdbc.primitives;

import br.commons.Result;
import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

@NullMarked
public record JDBCConnection (
        Connection delegate
) {

    public Result<Void, String> close() {
        try { delegate.close(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCStatement, String> createStatement() {
        try { return Result.success(new JDBCStatement(delegate.createStatement())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql) {
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCCallableStatement, String> prepareCall(String sql) {
        try { return Result.success(new JDBCCallableStatement(delegate.prepareCall(sql))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> nativeSQL(String sql) {
        try { return Result.success(delegate.nativeSQL(sql)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setAutoCommit(boolean autoCommit) {
        try { delegate.setAutoCommit(autoCommit); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getAutoCommit() {
        try { return Result.success(delegate.getAutoCommit()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> commit() {
        try { delegate.commit(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> rollback() {
        try { delegate.rollback(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isClosed() {
        try { return Result.success(delegate.isClosed()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCDatabaseMetaData, String> getMetaData() {
        try { return Result.success(new JDBCDatabaseMetaData(delegate.getMetaData())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setReadOnly(boolean readOnly) {
        try { delegate.setReadOnly(readOnly); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isReadOnly() {
        try { return Result.success(delegate.isReadOnly()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCatalog(String catalog) {
        try { delegate.setCatalog(catalog); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getCatalog() {
        try { return Result.success(delegate.getCatalog()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTransactionIsolation(int level) {
        try { delegate.setTransactionIsolation(level); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getTransactionIsolation() {
        try { return Result.success(delegate.getTransactionIsolation()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLWarning, String> getWarnings() {
        try { return Result.success(delegate.getWarnings()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> clearWarnings() {
        try { delegate.clearWarnings(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCStatement, String> createStatement(int resultSetType, int resultSetConcurrency) {
        try { return Result.success(new JDBCStatement(delegate.createStatement(resultSetType, resultSetConcurrency))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCCallableStatement, String> prepareCall(String sql, int resultSetType, int resultSetConcurrency) {
        try { return Result.success(new JDBCCallableStatement(delegate.prepareCall(sql, resultSetType, resultSetConcurrency))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Map<String, Class<?>>, String> getTypeMap() {
        try { return Result.success(delegate.getTypeMap()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTypeMap(Map<String, Class<?>> map) {
        try { delegate.setTypeMap(map); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setHoldability(int holdability) {
        try { delegate.setHoldability(holdability); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getHoldability() {
        try { return Result.success(delegate.getHoldability()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Savepoint, String> setSavepoint() {
        try { return Result.success(delegate.setSavepoint()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Savepoint, String> setSavepoint(String name) {
        try { return Result.success(delegate.setSavepoint(name)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> rollback(Savepoint savepoint) {
        try { delegate.rollback(savepoint); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> releaseSavepoint(Savepoint savepoint) {
        try { delegate.releaseSavepoint(savepoint); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCStatement, String> createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        try { return Result.success(new JDBCStatement(delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCCallableStatement, String> prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        try { return Result.success(new JDBCCallableStatement(delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, int autoGeneratedKeys) {
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, autoGeneratedKeys))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, int[] columnIndexes) {
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, columnIndexes))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, String[] columnNames) {
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, columnNames))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Clob, String> createClob() {
        try { return Result.success(delegate.createClob()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Blob, String> createBlob() {
        try { return Result.success(delegate.createBlob()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<NClob, String> createNClob() {
        try { return Result.success(delegate.createNClob()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLXML, String> createSQLXML() {
        try { return Result.success(delegate.createSQLXML()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isValid(int timeout) {
        try { return Result.success(delegate.isValid(timeout)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClientInfo(String name, String value) {
        try { delegate.setClientInfo(name, value); return Result.success(); }
        catch (SQLClientInfoException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClientInfo(Properties properties) {
        try { delegate.setClientInfo(properties); return Result.success(); }
        catch (SQLClientInfoException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getClientInfo(String name) {
        try { return Result.success(delegate.getClientInfo(name)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Properties, String> getClientInfo() {
        try { return Result.success(delegate.getClientInfo()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Array, String> createArrayOf(String typeName, Object[] elements) {
        try { return Result.success(delegate.createArrayOf(typeName, elements)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Struct, String> createStruct(String typeName, Object[] attributes) {
        try { return Result.success(delegate.createStruct(typeName, attributes)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setSchema(String schema) {
        try { delegate.setSchema(schema); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getSchema() {
        try { return Result.success(delegate.getSchema()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> abort(Executor executor) {
        try { delegate.abort(executor); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNetworkTimeout(Executor executor, int milliseconds) {
        try { delegate.setNetworkTimeout(executor, milliseconds); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getNetworkTimeout() {
        try { return Result.success(delegate.getNetworkTimeout()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public <T> Result<T, String> unwrap(Class<T> iface) {
        try { return Result.success(delegate.unwrap(iface)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isWrapperFor(Class<?> iface) {
        try { return Result.success(delegate.isWrapperFor(iface)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }
}

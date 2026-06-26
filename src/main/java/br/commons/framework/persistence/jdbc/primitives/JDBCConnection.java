package br.commons.framework.persistence.jdbc.primitives;

import br.commons.Logger;
import br.commons.Result;
import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;

import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

@NullMarked
public record JDBCConnection (
        Connection delegate
) {

    public Result<Void, String> close() {
        Logger.trace("close()");
        try { delegate.close(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCStatement, String> createStatement() {
        Logger.trace("createStatement()");
        try { return Result.success(new JDBCStatement(delegate.createStatement())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql) {
        Logger.trace("prepareStatement(%s)", sql);
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCCallableStatement, String> prepareCall(String sql) {
        Logger.trace("prepareCall(%s)", sql);
        try { return Result.success(new JDBCCallableStatement(delegate.prepareCall(sql))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> nativeSQL(String sql) {
        Logger.trace("nativeSQL(%s)", sql);
        try { return Result.success(delegate.nativeSQL(sql)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setAutoCommit(boolean autoCommit) {
        Logger.trace("setAutoCommit(%s)", autoCommit);
        try { delegate.setAutoCommit(autoCommit); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getAutoCommit() {
        Logger.trace("getAutoCommit");
        try { return Result.success(delegate.getAutoCommit()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> commit() {
        Logger.trace("commit()");
        try { delegate.commit(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> rollback() {
        Logger.trace("rollback()");
        try { delegate.rollback(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isClosed() {
        Logger.trace("isClosed()");
        try { return Result.success(delegate.isClosed()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCMetaData, String> getMetaData() {
        Logger.trace("getMetaData");
        try { return Result.success(new JDBCMetaData(delegate.getMetaData())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setReadOnly(boolean readOnly) {
        Logger.trace("setReadOnly(%s)", readOnly);
        try { delegate.setReadOnly(readOnly); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isReadOnly() {
        Logger.trace("isReadOnly()");
        try { return Result.success(delegate.isReadOnly()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCatalog(String catalog) {
        Logger.trace("setCatalog(%s)", catalog);
        try { delegate.setCatalog(catalog); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getCatalog() {
        Logger.trace("getCatalog");
        try { return Result.success(delegate.getCatalog()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTransactionIsolation(int level) {
        Logger.trace("setTransactionIsolation(%s)", Logger.lazy(() -> JDBCConstants.transactionIsolation(level)));
        try { delegate.setTransactionIsolation(level); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getTransactionIsolation() {
        Logger.trace("getTransactionIsolation");
        try { return Result.success(delegate.getTransactionIsolation()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLWarning, String> getWarnings() {
        Logger.trace("getWarnings");
        try { return Result.success(delegate.getWarnings()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> clearWarnings() {
        Logger.trace("clearWarnings()");
        try { delegate.clearWarnings(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCStatement, String> createStatement(int resultSetType, int resultSetConcurrency) {
        Logger.trace("createStatement(%s, %s)", Logger.lazy(() -> JDBCConstants.resultSetType(resultSetType)), Logger.lazy(() -> JDBCConstants.concurrency(resultSetConcurrency)));
        try { return Result.success(new JDBCStatement(delegate.createStatement(resultSetType, resultSetConcurrency))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
        Logger.trace("prepareStatement(%s, %s, %s)", sql, Logger.lazy(() -> JDBCConstants.resultSetType(resultSetType)), Logger.lazy(() -> JDBCConstants.concurrency(resultSetConcurrency)));
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCCallableStatement, String> prepareCall(String sql, int resultSetType, int resultSetConcurrency) {
        Logger.trace("prepareCall(%s, %s, %s)", sql, Logger.lazy(() -> JDBCConstants.resultSetType(resultSetType)), Logger.lazy(() -> JDBCConstants.concurrency(resultSetConcurrency)));
        try { return Result.success(new JDBCCallableStatement(delegate.prepareCall(sql, resultSetType, resultSetConcurrency))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Map<String, Class<?>>, String> getTypeMap() {
        Logger.trace("getTypeMap");
        try { return Result.success(delegate.getTypeMap()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTypeMap(Map<String, Class<?>> map) {
        Logger.trace("setTypeMap(%s)", map);
        try { delegate.setTypeMap(map); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setHoldability(int holdability) {
        Logger.trace("setHoldability(%s)", Logger.lazy(() -> JDBCConstants.holdability(holdability)));
        try { delegate.setHoldability(holdability); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getHoldability() {
        Logger.trace("getHoldability");
        try { return Result.success(delegate.getHoldability()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Savepoint, String> setSavepoint() {
        Logger.trace("setSavepoint()");
        try { return Result.success(delegate.setSavepoint()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Savepoint, String> setSavepoint(String name) {
        Logger.trace("setSavepoint(%s)", name);
        try { return Result.success(delegate.setSavepoint(name)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> rollback(Savepoint savepoint) {
        Logger.trace("rollback(%s)", Logger.lazy(() -> {
            try {
                return savepoint.getSavepointName();
            } catch (SQLException e) {
                return e.getMessage();
            }
        }));
        try { delegate.rollback(savepoint); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> releaseSavepoint(Savepoint savepoint) {
        Logger.trace("releaseSavepoint(%s)", Logger.lazy(() -> {
            try {
                return savepoint.getSavepointName();
            } catch (SQLException e) {
                return e.getMessage();
            }
        }));
        try { delegate.releaseSavepoint(savepoint); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCStatement, String> createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        Logger.trace("createStatement(%s, %s, %s)", Logger.lazy(() -> JDBCConstants.resultSetType(resultSetType)), Logger.lazy(() -> JDBCConstants.concurrency(resultSetConcurrency)), Logger.lazy(() -> JDBCConstants.holdability(resultSetHoldability)));
        try { return Result.success(new JDBCStatement(delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        Logger.trace("prepareStatement(%s, %s, %s)", Logger.lazy(() -> JDBCConstants.resultSetType(resultSetType)), Logger.lazy(() -> JDBCConstants.concurrency(resultSetConcurrency)), Logger.lazy(() -> JDBCConstants.holdability(resultSetHoldability)));
        Logger.verbose(sql);
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCCallableStatement, String> prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        Logger.trace("prepareCall(%s, %s, %s)", Logger.lazy(() -> JDBCConstants.resultSetType(resultSetType)), Logger.lazy(() -> JDBCConstants.concurrency(resultSetConcurrency)), Logger.lazy(() -> JDBCConstants.holdability(resultSetHoldability)));
        Logger.verbose(sql);
        try { return Result.success(new JDBCCallableStatement(delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, int autoGeneratedKeys) {
        Logger.trace("prepareStatement(%s, %s)", sql, Logger.lazy(() -> JDBCConstants.generatedKeys(autoGeneratedKeys)));
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, autoGeneratedKeys))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, int[] columnIndexes) {
        Logger.trace("prepareStatement(%s, %s)", sql, Logger.lazy(() -> Arrays.toString(columnIndexes)));
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, columnIndexes))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCPreparedStatement, String> prepareStatement(String sql, String[] columnNames) {
        Logger.trace("prepareStatement(%s, %s)", sql, Logger.lazy(() -> Arrays.toString(columnNames)));
        try { return Result.success(new JDBCPreparedStatement(delegate.prepareStatement(sql, columnNames))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Clob, String> createClob() {
        Logger.trace("createClob()");
        try { return Result.success(delegate.createClob()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Blob, String> createBlob() {
        Logger.trace("createBlob()");
        try { return Result.success(delegate.createBlob()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<NClob, String> createNClob() {
        Logger.trace("createNClob()");
        try { return Result.success(delegate.createNClob()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLXML, String> createSQLXML() {
        Logger.trace("createSQLXML()");
        try { return Result.success(delegate.createSQLXML()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isValid(int timeout) {
        Logger.trace("isValid(%s)", timeout);
        try { return Result.success(delegate.isValid(timeout)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClientInfo(String name, String value) {
        Logger.trace("setClientInfo(%s, %s)", name, value);
        try { delegate.setClientInfo(name, value); return Result.success(); }
        catch (SQLClientInfoException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClientInfo(Properties properties) {
        Logger.trace("setClientInfo(%s)", properties);
        try { delegate.setClientInfo(properties); return Result.success(); }
        catch (SQLClientInfoException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getClientInfo(String name) {
        Logger.trace("getClientInfo");
        try { return Result.success(delegate.getClientInfo(name)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Properties, String> getClientInfo() {
        Logger.trace("getClientInfo");
        try { return Result.success(delegate.getClientInfo()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Array, String> createArrayOf(String typeName, Object[] elements) {
        Logger.trace("createArrayOf(%s, %s)", typeName, Logger.lazy(() -> Arrays.toString(elements)));
        try { return Result.success(delegate.createArrayOf(typeName, elements)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Struct, String> createStruct(String typeName, Object[] attributes) {
        Logger.trace("createStruct(%s, %s)", typeName, Logger.lazy(() -> Arrays.toString(attributes)));
        try { return Result.success(delegate.createStruct(typeName, attributes)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setSchema(String schema) {
        Logger.trace("setSchema(%s)", schema);
        try { delegate.setSchema(schema); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getSchema() {
        Logger.trace("getSchema");
        try { return Result.success(delegate.getSchema()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> abort(Executor executor) {
        Logger.trace("abort(%s)", executor);
        try { delegate.abort(executor); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNetworkTimeout(Executor executor, int milliseconds) {
        Logger.trace("setNetworkTimeout(%s, %s)", executor, milliseconds);
        try { delegate.setNetworkTimeout(executor, milliseconds); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getNetworkTimeout() {
        Logger.trace("getNetworkTimeout");
        try { return Result.success(delegate.getNetworkTimeout()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public <T> Result<T, String> unwrap(Class<T> iface) {
        Logger.trace("unwrap(%s)", iface);
        try { return Result.success(delegate.unwrap(iface)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isWrapperFor(Class<?> iface) {
        Logger.trace("isWrapperFor(%s)", iface);
        try { return Result.success(delegate.isWrapperFor(iface)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }
}

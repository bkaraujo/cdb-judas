package br.commons.framework.persistence.jdbc.primitives;

import br.commons.Result;
import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@NullMarked
public record JDBCMetaData(
        DatabaseMetaData delegate
) {

    public Result<Boolean, String> allProceduresAreCallable() {
        try { return Result.success(delegate.allProceduresAreCallable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> allTablesAreSelectable() {
        try { return Result.success(delegate.allTablesAreSelectable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getURL() {
        try { return Result.success(delegate.getURL()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getUserName() {
        try { return Result.success(delegate.getUserName()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isReadOnly() {
        try { return Result.success(delegate.isReadOnly()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> nullsAreSortedHigh() {
        try { return Result.success(delegate.nullsAreSortedHigh()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> nullsAreSortedLow() {
        try { return Result.success(delegate.nullsAreSortedLow()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> nullsAreSortedAtStart() {
        try { return Result.success(delegate.nullsAreSortedAtStart()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> nullsAreSortedAtEnd() {
        try { return Result.success(delegate.nullsAreSortedAtEnd()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getDatabaseProductName() {
        try { return Result.success(delegate.getDatabaseProductName()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getDatabaseProductVersion() {
        try { return Result.success(delegate.getDatabaseProductVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getDriverName() {
        try { return Result.success(delegate.getDriverName()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getDriverVersion() {
        try { return Result.success(delegate.getDriverVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getDriverMajorVersion() {
        return Result.success(delegate.getDriverMajorVersion());
    }

    public Result<Integer, String> getDriverMinorVersion() {
        return Result.success(delegate.getDriverMinorVersion());
    }

    public Result<Boolean, String> usesLocalFiles() {
        try { return Result.success(delegate.usesLocalFiles()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> usesLocalFilePerTable() {
        try { return Result.success(delegate.usesLocalFilePerTable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsMixedCaseIdentifiers() {
        try { return Result.success(delegate.supportsMixedCaseIdentifiers()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> storesUpperCaseIdentifiers() {
        try { return Result.success(delegate.storesUpperCaseIdentifiers()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> storesLowerCaseIdentifiers() {
        try { return Result.success(delegate.storesLowerCaseIdentifiers()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> storesMixedCaseIdentifiers() {
        try { return Result.success(delegate.storesMixedCaseIdentifiers()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getIdentifierQuoteString() {
        try { return Result.success(delegate.getIdentifierQuoteString()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getSQLKeywords() {
        try { return Result.success(delegate.getSQLKeywords()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getNumericFunctions() {
        try { return Result.success(delegate.getNumericFunctions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getStringFunctions() {
        try { return Result.success(delegate.getStringFunctions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getSystemFunctions() {
        try { return Result.success(delegate.getSystemFunctions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getTimeDateFunctions() {
        try { return Result.success(delegate.getTimeDateFunctions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getSearchStringEscape() {
        try { return Result.success(delegate.getSearchStringEscape()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getExtraNameCharacters() {
        try { return Result.success(delegate.getExtraNameCharacters()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsAlterTableWithAddColumn() {
        try { return Result.success(delegate.supportsAlterTableWithAddColumn()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsAlterTableWithDropColumn() {
        try { return Result.success(delegate.supportsAlterTableWithDropColumn()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsColumnAliasing() {
        try { return Result.success(delegate.supportsColumnAliasing()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsTransactions() {
        try { return Result.success(delegate.supportsTransactions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getDefaultTransactionIsolation() {
        try { return Result.success(delegate.getDefaultTransactionIsolation()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsTransactionIsolationLevel(int level) {
        try { return Result.success(delegate.supportsTransactionIsolationLevel(level)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsStoredProcedures() {
        try { return Result.success(delegate.supportsStoredProcedures()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxBinaryLiteralLength() {
        try { return Result.success(delegate.getMaxBinaryLiteralLength()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxCharLiteralLength() {
        try { return Result.success(delegate.getMaxCharLiteralLength()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnNameLength() {
        try { return Result.success(delegate.getMaxColumnNameLength()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInGroupBy() {
        try { return Result.success(delegate.getMaxColumnsInGroupBy()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInIndex() {
        try { return Result.success(delegate.getMaxColumnsInIndex()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInOrderBy() {
        try { return Result.success(delegate.getMaxColumnsInOrderBy()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInSelect() {
        try { return Result.success(delegate.getMaxColumnsInSelect()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInTable() {
        try { return Result.success(delegate.getMaxColumnsInTable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxConnections() {
        try { return Result.success(delegate.getMaxConnections()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxTableNameLength() {
        try { return Result.success(delegate.getMaxTableNameLength()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxTablesInSelect() {
        try { return Result.success(delegate.getMaxTablesInSelect()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCConnection, String> getConnection() {
        try { return Result.success(new JDBCConnection(delegate.getConnection())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getProcedures(String catalog, String schemaPattern, String procedureNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getProcedures(catalog, schemaPattern, procedureNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) {
        try { return Result.success(new JDBCResultSet(delegate.getTables(catalog, schemaPattern, tableNamePattern, types))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getSchemas() {
        try { return Result.success(new JDBCResultSet(delegate.getSchemas())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getCatalogs() {
        try { return Result.success(new JDBCResultSet(delegate.getCatalogs())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getTableTypes() {
        try { return Result.success(new JDBCResultSet(delegate.getTableTypes())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getColumnPrivileges(catalog, schema, table, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getTablePrivileges(catalog, schemaPattern, tableNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) {
        try { return Result.success(new JDBCResultSet(delegate.getBestRowIdentifier(catalog, schema, table, scope, nullable))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getVersionColumns(String catalog, String schema, String table) {
        try { return Result.success(new JDBCResultSet(delegate.getVersionColumns(catalog, schema, table))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getPrimaryKeys(String catalog, String schema, String table) {
        try { return Result.success(new JDBCResultSet(delegate.getPrimaryKeys(catalog, schema, table))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getImportedKeys(String catalog, String schema, String table) {
        try { return Result.success(new JDBCResultSet(delegate.getImportedKeys(catalog, schema, table))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getExportedKeys(String catalog, String schema, String table) {
        try { return Result.success(new JDBCResultSet(delegate.getExportedKeys(catalog, schema, table))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) {
        try { return Result.success(new JDBCResultSet(delegate.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getTypeInfo() {
        try { return Result.success(new JDBCResultSet(delegate.getTypeInfo())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) {
        try { return Result.success(new JDBCResultSet(delegate.getIndexInfo(catalog, schema, table, unique, approximate))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsResultSetType(int type) {
        try { return Result.success(delegate.supportsResultSetType(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsResultSetConcurrency(int type, int concurrency) {
        try { return Result.success(delegate.supportsResultSetConcurrency(type, concurrency)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> ownUpdatesAreVisible(int type) {
        try { return Result.success(delegate.ownUpdatesAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> ownDeletesAreVisible(int type) {
        try { return Result.success(delegate.ownDeletesAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> ownInsertsAreVisible(int type) {
        try { return Result.success(delegate.ownInsertsAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> othersUpdatesAreVisible(int type) {
        try { return Result.success(delegate.othersUpdatesAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> othersDeletesAreVisible(int type) {
        try { return Result.success(delegate.othersDeletesAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> othersInsertsAreVisible(int type) {
        try { return Result.success(delegate.othersInsertsAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> updatesAreDetected(int type) {
        try { return Result.success(delegate.updatesAreDetected(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> deletesAreDetected(int type) {
        try { return Result.success(delegate.deletesAreDetected(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> insertsAreDetected(int type) {
        try { return Result.success(delegate.insertsAreDetected(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsBatchUpdates() {
        try { return Result.success(delegate.supportsBatchUpdates()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) {
        try { return Result.success(new JDBCResultSet(delegate.getUDTs(catalog, schemaPattern, typeNamePattern, types))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsSavepoints() {
        try { return Result.success(delegate.supportsSavepoints()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsNamedParameters() {
        try { return Result.success(delegate.supportsNamedParameters()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsMultipleOpenResults() {
        try { return Result.success(delegate.supportsMultipleOpenResults()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsGetGeneratedKeys() {
        try { return Result.success(delegate.supportsGetGeneratedKeys()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getSuperTypes(catalog, schemaPattern, typeNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getSuperTables(String catalog, String schemaPattern, String tableNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getSuperTables(catalog, schemaPattern, tableNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsResultSetHoldability(int holdability) {
        try { return Result.success(delegate.supportsResultSetHoldability(holdability)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getResultSetHoldability() {
        try { return Result.success(delegate.getResultSetHoldability()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getDatabaseMajorVersion() {
        try { return Result.success(delegate.getDatabaseMajorVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getDatabaseMinorVersion() {
        try { return Result.success(delegate.getDatabaseMinorVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getJDBCMajorVersion() {
        try { return Result.success(delegate.getJDBCMajorVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getJDBCMinorVersion() {
        try { return Result.success(delegate.getJDBCMinorVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getSQLStateType() {
        try { return Result.success(delegate.getSQLStateType()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> locatorsUpdateCopy() {
        try { return Result.success(delegate.locatorsUpdateCopy()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsStatementPooling() {
        try { return Result.success(delegate.supportsStatementPooling()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getSchemas(String catalog, String schemaPattern) {
        try { return Result.success(new JDBCResultSet(delegate.getSchemas(catalog, schemaPattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsStoredFunctionsUsingCallSyntax() {
        try { return Result.success(delegate.supportsStoredFunctionsUsingCallSyntax()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> autoCommitFailureClosesAllResultSets() {
        try { return Result.success(delegate.autoCommitFailureClosesAllResultSets()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getClientInfoProperties() {
        try { return Result.success(new JDBCResultSet(delegate.getClientInfoProperties())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getFunctions(String catalog, String schemaPattern, String functionNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getFunctions(catalog, schemaPattern, functionNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) {
        try { return Result.success(new JDBCResultSet(delegate.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> generatedKeyAlwaysReturned() {
        try { return Result.success(delegate.generatedKeyAlwaysReturned()); }
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

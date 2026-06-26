package br.commons.framework.persistence.jdbc.primitives;

import br.commons.Logger;
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
        Logger.trace("allProceduresAreCallable()");
        try { return Result.success(delegate.allProceduresAreCallable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> allTablesAreSelectable() {
        Logger.trace("allTablesAreSelectable()");
        try { return Result.success(delegate.allTablesAreSelectable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getURL() {
        Logger.trace("getURL");
        try { return Result.success(delegate.getURL()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getUserName() {
        Logger.trace("getUserName");
        try { return Result.success(delegate.getUserName()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isReadOnly() {
        Logger.trace("isReadOnly()");
        try { return Result.success(delegate.isReadOnly()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> nullsAreSortedHigh() {
        Logger.trace("nullsAreSortedHigh()");
        try { return Result.success(delegate.nullsAreSortedHigh()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> nullsAreSortedLow() {
        Logger.trace("nullsAreSortedLow()");
        try { return Result.success(delegate.nullsAreSortedLow()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> nullsAreSortedAtStart() {
        Logger.trace("nullsAreSortedAtStart()");
        try { return Result.success(delegate.nullsAreSortedAtStart()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> nullsAreSortedAtEnd() {
        Logger.trace("nullsAreSortedAtEnd()");
        try { return Result.success(delegate.nullsAreSortedAtEnd()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getDatabaseProductName() {
        Logger.trace("getDatabaseProductName");
        try { return Result.success(delegate.getDatabaseProductName()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getDatabaseProductVersion() {
        Logger.trace("getDatabaseProductVersion");
        try { return Result.success(delegate.getDatabaseProductVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getDriverName() {
        Logger.trace("getDriverName");
        try { return Result.success(delegate.getDriverName()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getDriverVersion() {
        Logger.trace("getDriverVersion");
        try { return Result.success(delegate.getDriverVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getDriverMajorVersion() {
        Logger.trace("getDriverMajorVersion");
        return Result.success(delegate.getDriverMajorVersion());
    }

    public Result<Integer, String> getDriverMinorVersion() {
        Logger.trace("getDriverMinorVersion");
        return Result.success(delegate.getDriverMinorVersion());
    }

    public Result<Boolean, String> usesLocalFiles() {
        Logger.trace("usesLocalFiles()");
        try { return Result.success(delegate.usesLocalFiles()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> usesLocalFilePerTable() {
        Logger.trace("usesLocalFilePerTable()");
        try { return Result.success(delegate.usesLocalFilePerTable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsMixedCaseIdentifiers() {
        Logger.trace("supportsMixedCaseIdentifiers()");
        try { return Result.success(delegate.supportsMixedCaseIdentifiers()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> storesUpperCaseIdentifiers() {
        Logger.trace("storesUpperCaseIdentifiers()");
        try { return Result.success(delegate.storesUpperCaseIdentifiers()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> storesLowerCaseIdentifiers() {
        Logger.trace("storesLowerCaseIdentifiers()");
        try { return Result.success(delegate.storesLowerCaseIdentifiers()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> storesMixedCaseIdentifiers() {
        Logger.trace("storesMixedCaseIdentifiers()");
        try { return Result.success(delegate.storesMixedCaseIdentifiers()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getIdentifierQuoteString() {
        Logger.trace("getIdentifierQuoteString");
        try { return Result.success(delegate.getIdentifierQuoteString()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getSQLKeywords() {
        Logger.trace("getSQLKeywords");
        try { return Result.success(delegate.getSQLKeywords()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getNumericFunctions() {
        Logger.trace("getNumericFunctions");
        try { return Result.success(delegate.getNumericFunctions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getStringFunctions() {
        Logger.trace("getStringFunctions");
        try { return Result.success(delegate.getStringFunctions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getSystemFunctions() {
        Logger.trace("getSystemFunctions");
        try { return Result.success(delegate.getSystemFunctions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getTimeDateFunctions() {
        Logger.trace("getTimeDateFunctions");
        try { return Result.success(delegate.getTimeDateFunctions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getSearchStringEscape() {
        Logger.trace("getSearchStringEscape");
        try { return Result.success(delegate.getSearchStringEscape()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getExtraNameCharacters() {
        Logger.trace("getExtraNameCharacters");
        try { return Result.success(delegate.getExtraNameCharacters()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsAlterTableWithAddColumn() {
        Logger.trace("supportsAlterTableWithAddColumn()");
        try { return Result.success(delegate.supportsAlterTableWithAddColumn()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsAlterTableWithDropColumn() {
        Logger.trace("supportsAlterTableWithDropColumn()");
        try { return Result.success(delegate.supportsAlterTableWithDropColumn()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsColumnAliasing() {
        Logger.trace("supportsColumnAliasing()");
        try { return Result.success(delegate.supportsColumnAliasing()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsTransactions() {
        Logger.trace("supportsTransactions()");
        try { return Result.success(delegate.supportsTransactions()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getDefaultTransactionIsolation() {
        Logger.trace("getDefaultTransactionIsolation");
        try { return Result.success(delegate.getDefaultTransactionIsolation()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsTransactionIsolationLevel(int level) {
        Logger.trace("supportsTransactionIsolationLevel(%s)", Logger.lazy(() -> JDBCConstants.transactionIsolation(level)));
        try { return Result.success(delegate.supportsTransactionIsolationLevel(level)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsStoredProcedures() {
        Logger.trace("supportsStoredProcedures()");
        try { return Result.success(delegate.supportsStoredProcedures()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxBinaryLiteralLength() {
        Logger.trace("getMaxBinaryLiteralLength");
        try { return Result.success(delegate.getMaxBinaryLiteralLength()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxCharLiteralLength() {
        Logger.trace("getMaxCharLiteralLength");
        try { return Result.success(delegate.getMaxCharLiteralLength()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnNameLength() {
        Logger.trace("getMaxColumnNameLength");
        try { return Result.success(delegate.getMaxColumnNameLength()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInGroupBy() {
        Logger.trace("getMaxColumnsInGroupBy");
        try { return Result.success(delegate.getMaxColumnsInGroupBy()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInIndex() {
        Logger.trace("getMaxColumnsInIndex");
        try { return Result.success(delegate.getMaxColumnsInIndex()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInOrderBy() {
        Logger.trace("getMaxColumnsInOrderBy");
        try { return Result.success(delegate.getMaxColumnsInOrderBy()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInSelect() {
        Logger.trace("getMaxColumnsInSelect");
        try { return Result.success(delegate.getMaxColumnsInSelect()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxColumnsInTable() {
        Logger.trace("getMaxColumnsInTable");
        try { return Result.success(delegate.getMaxColumnsInTable()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxConnections() {
        Logger.trace("getMaxConnections");
        try { return Result.success(delegate.getMaxConnections()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxTableNameLength() {
        Logger.trace("getMaxTableNameLength");
        try { return Result.success(delegate.getMaxTableNameLength()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxTablesInSelect() {
        Logger.trace("getMaxTablesInSelect");
        try { return Result.success(delegate.getMaxTablesInSelect()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCConnection, String> getConnection() {
        Logger.trace("getConnection");
        try { return Result.success(new JDBCConnection(delegate.getConnection())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getProcedures(String catalog, String schemaPattern, String procedureNamePattern) {
        Logger.trace("getProcedures");
        try { return Result.success(new JDBCResultSet(delegate.getProcedures(catalog, schemaPattern, procedureNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) {
        Logger.trace("getProcedureColumns");
        try { return Result.success(new JDBCResultSet(delegate.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) {
        Logger.trace("getTables");
        try { return Result.success(new JDBCResultSet(delegate.getTables(catalog, schemaPattern, tableNamePattern, types))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getSchemas() {
        Logger.trace("getSchemas");
        try { return Result.success(new JDBCResultSet(delegate.getSchemas())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getCatalogs() {
        Logger.trace("getCatalogs");
        try { return Result.success(new JDBCResultSet(delegate.getCatalogs())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getTableTypes() {
        Logger.trace("getTableTypes");
        try { return Result.success(new JDBCResultSet(delegate.getTableTypes())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) {
        Logger.trace("getColumns");
        try { return Result.success(new JDBCResultSet(delegate.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) {
        Logger.trace("getColumnPrivileges");
        try { return Result.success(new JDBCResultSet(delegate.getColumnPrivileges(catalog, schema, table, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) {
        Logger.trace("getTablePrivileges");
        try { return Result.success(new JDBCResultSet(delegate.getTablePrivileges(catalog, schemaPattern, tableNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) {
        Logger.trace("getBestRowIdentifier");
        try { return Result.success(new JDBCResultSet(delegate.getBestRowIdentifier(catalog, schema, table, scope, nullable))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getVersionColumns(String catalog, String schema, String table) {
        Logger.trace("getVersionColumns");
        try { return Result.success(new JDBCResultSet(delegate.getVersionColumns(catalog, schema, table))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getPrimaryKeys(String catalog, String schema, String table) {
        Logger.trace("getPrimaryKeys");
        try { return Result.success(new JDBCResultSet(delegate.getPrimaryKeys(catalog, schema, table))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getImportedKeys(String catalog, String schema, String table) {
        Logger.trace("getImportedKeys");
        try { return Result.success(new JDBCResultSet(delegate.getImportedKeys(catalog, schema, table))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getExportedKeys(String catalog, String schema, String table) {
        Logger.trace("getExportedKeys");
        try { return Result.success(new JDBCResultSet(delegate.getExportedKeys(catalog, schema, table))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) {
        Logger.trace("getCrossReference");
        try { return Result.success(new JDBCResultSet(delegate.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getTypeInfo() {
        Logger.trace("getTypeInfo");
        try { return Result.success(new JDBCResultSet(delegate.getTypeInfo())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) {
        Logger.trace("getIndexInfo");
        try { return Result.success(new JDBCResultSet(delegate.getIndexInfo(catalog, schema, table, unique, approximate))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsResultSetType(int type) {
        Logger.trace("supportsResultSetType(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.supportsResultSetType(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsResultSetConcurrency(int type, int concurrency) {
        Logger.trace("supportsResultSetConcurrency(%s, %s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)), Logger.lazy(() -> JDBCConstants.concurrency(concurrency)));
        try { return Result.success(delegate.supportsResultSetConcurrency(type, concurrency)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> ownUpdatesAreVisible(int type) {
        Logger.trace("ownUpdatesAreVisible(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.ownUpdatesAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> ownDeletesAreVisible(int type) {
        Logger.trace("ownDeletesAreVisible(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.ownDeletesAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> ownInsertsAreVisible(int type) {
        Logger.trace("ownInsertsAreVisible(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.ownInsertsAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> othersUpdatesAreVisible(int type) {
        Logger.trace("othersUpdatesAreVisible(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.othersUpdatesAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> othersDeletesAreVisible(int type) {
        Logger.trace("othersDeletesAreVisible(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.othersDeletesAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> othersInsertsAreVisible(int type) {
        Logger.trace("othersInsertsAreVisible(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.othersInsertsAreVisible(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> updatesAreDetected(int type) {
        Logger.trace("updatesAreDetected(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.updatesAreDetected(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> deletesAreDetected(int type) {
        Logger.trace("deletesAreDetected(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.deletesAreDetected(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> insertsAreDetected(int type) {
        Logger.trace("insertsAreDetected(%s)", Logger.lazy(() -> JDBCConstants.resultSetType(type)));
        try { return Result.success(delegate.insertsAreDetected(type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsBatchUpdates() {
        Logger.trace("supportsBatchUpdates()");
        try { return Result.success(delegate.supportsBatchUpdates()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) {
        Logger.trace("getUDTs");
        try { return Result.success(new JDBCResultSet(delegate.getUDTs(catalog, schemaPattern, typeNamePattern, types))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsSavepoints() {
        Logger.trace("supportsSavepoints()");
        try { return Result.success(delegate.supportsSavepoints()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsNamedParameters() {
        Logger.trace("supportsNamedParameters()");
        try { return Result.success(delegate.supportsNamedParameters()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsMultipleOpenResults() {
        Logger.trace("supportsMultipleOpenResults()");
        try { return Result.success(delegate.supportsMultipleOpenResults()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsGetGeneratedKeys() {
        Logger.trace("supportsGetGeneratedKeys()");
        try { return Result.success(delegate.supportsGetGeneratedKeys()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) {
        Logger.trace("getSuperTypes");
        try { return Result.success(new JDBCResultSet(delegate.getSuperTypes(catalog, schemaPattern, typeNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getSuperTables(String catalog, String schemaPattern, String tableNamePattern) {
        Logger.trace("getSuperTables");
        try { return Result.success(new JDBCResultSet(delegate.getSuperTables(catalog, schemaPattern, tableNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) {
        Logger.trace("getAttributes");
        try { return Result.success(new JDBCResultSet(delegate.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsResultSetHoldability(int holdability) {
        Logger.trace("supportsResultSetHoldability(%s)", Logger.lazy(() -> JDBCConstants.holdability(holdability)));
        try { return Result.success(delegate.supportsResultSetHoldability(holdability)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getResultSetHoldability() {
        Logger.trace("getResultSetHoldability");
        try { return Result.success(delegate.getResultSetHoldability()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getDatabaseMajorVersion() {
        Logger.trace("getDatabaseMajorVersion");
        try { return Result.success(delegate.getDatabaseMajorVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getDatabaseMinorVersion() {
        Logger.trace("getDatabaseMinorVersion");
        try { return Result.success(delegate.getDatabaseMinorVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getJDBCMajorVersion() {
        Logger.trace("getJDBCMajorVersion");
        try { return Result.success(delegate.getJDBCMajorVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getJDBCMinorVersion() {
        Logger.trace("getJDBCMinorVersion");
        try { return Result.success(delegate.getJDBCMinorVersion()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getSQLStateType() {
        Logger.trace("getSQLStateType");
        try { return Result.success(delegate.getSQLStateType()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> locatorsUpdateCopy() {
        Logger.trace("locatorsUpdateCopy()");
        try { return Result.success(delegate.locatorsUpdateCopy()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsStatementPooling() {
        Logger.trace("supportsStatementPooling()");
        try { return Result.success(delegate.supportsStatementPooling()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getSchemas(String catalog, String schemaPattern) {
        Logger.trace("getSchemas");
        try { return Result.success(new JDBCResultSet(delegate.getSchemas(catalog, schemaPattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> supportsStoredFunctionsUsingCallSyntax() {
        Logger.trace("supportsStoredFunctionsUsingCallSyntax()");
        try { return Result.success(delegate.supportsStoredFunctionsUsingCallSyntax()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> autoCommitFailureClosesAllResultSets() {
        Logger.trace("autoCommitFailureClosesAllResultSets()");
        try { return Result.success(delegate.autoCommitFailureClosesAllResultSets()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getClientInfoProperties() {
        Logger.trace("getClientInfoProperties");
        try { return Result.success(new JDBCResultSet(delegate.getClientInfoProperties())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getFunctions(String catalog, String schemaPattern, String functionNamePattern) {
        Logger.trace("getFunctions");
        try { return Result.success(new JDBCResultSet(delegate.getFunctions(catalog, schemaPattern, functionNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) {
        Logger.trace("getFunctionColumns");
        try { return Result.success(new JDBCResultSet(delegate.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) {
        Logger.trace("getPseudoColumns");
        try { return Result.success(new JDBCResultSet(delegate.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern))); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> generatedKeyAlwaysReturned() {
        Logger.trace("generatedKeyAlwaysReturned()");
        try { return Result.success(delegate.generatedKeyAlwaysReturned()); }
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

package br.commons.framework.persistence.jdbc.primitives;

import br.commons.Logger;
import br.commons.Result;
import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

@NullMarked
public record JDBCResultSet(
        ResultSet delegate
) {

    public Result<Boolean, String> next() {
        Logger.trace("next");
        try { return Result.success(delegate.next()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> close() {
        Logger.trace("close");
        try { delegate.close(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> wasNull() {
        Logger.trace("wasNull");
        try { return Result.success(delegate.wasNull()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getString(int columnIndex) {
        try { return Result.success(delegate.getString(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getBoolean(int columnIndex) {
        try { return Result.success(delegate.getBoolean(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Byte, String> getByte(int columnIndex) {
        try { return Result.success(delegate.getByte(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Short, String> getShort(int columnIndex) {
        try { return Result.success(delegate.getShort(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getInt(int columnIndex) {
        try { return Result.success(delegate.getInt(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Long, String> getLong(int columnIndex) {
        try { return Result.success(delegate.getLong(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Float, String> getFloat(int columnIndex) {
        try { return Result.success(delegate.getFloat(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Double, String> getDouble(int columnIndex) {
        try { return Result.success(delegate.getDouble(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<BigDecimal, String> getBigDecimal(int columnIndex) {
        try { return Result.success(delegate.getBigDecimal(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<byte[], String> getBytes(int columnIndex) {
        try { return Result.success(delegate.getBytes(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Date, String> getDate(int columnIndex) {
        try { return Result.success(delegate.getDate(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Time, String> getTime(int columnIndex) {
        try { return Result.success(delegate.getTime(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Timestamp, String> getTimestamp(int columnIndex) {
        try { return Result.success(delegate.getTimestamp(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<InputStream, String> getAsciiStream(int columnIndex) {
        try { return Result.success(delegate.getAsciiStream(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<InputStream, String> getBinaryStream(int columnIndex) {
        try { return Result.success(delegate.getBinaryStream(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getString(String columnLabel) {
        try { return Result.success(delegate.getString(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getBoolean(String columnLabel) {
        try { return Result.success(delegate.getBoolean(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Byte, String> getByte(String columnLabel) {
        try { return Result.success(delegate.getByte(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Short, String> getShort(String columnLabel) {
        try { return Result.success(delegate.getShort(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getInt(String columnLabel) {
        try { return Result.success(delegate.getInt(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Long, String> getLong(String columnLabel) {
        try { return Result.success(delegate.getLong(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Float, String> getFloat(String columnLabel) {
        try { return Result.success(delegate.getFloat(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Double, String> getDouble(String columnLabel) {
        try { return Result.success(delegate.getDouble(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<BigDecimal, String> getBigDecimal(String columnLabel) {
        try { return Result.success(delegate.getBigDecimal(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<byte[], String> getBytes(String columnLabel) {
        try { return Result.success(delegate.getBytes(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Date, String> getDate(String columnLabel) {
        try { return Result.success(delegate.getDate(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Time, String> getTime(String columnLabel) {
        try { return Result.success(delegate.getTime(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Timestamp, String> getTimestamp(String columnLabel) {
        try { return Result.success(delegate.getTimestamp(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<InputStream, String> getAsciiStream(String columnLabel) {
        try { return Result.success(delegate.getAsciiStream(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<InputStream, String> getBinaryStream(String columnLabel) {
        try { return Result.success(delegate.getBinaryStream(columnLabel)); }
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

    public Result<String, String> getCursorName() {
        Logger.trace("getCursorName");
        try { return Result.success(delegate.getCursorName()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<ResultSetMetaData, String> getMetaData() {
        Logger.trace("getMetaData");
        try { return Result.success(delegate.getMetaData()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Object, String> getObject(int columnIndex) {
        try { return Result.success(delegate.getObject(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Object, String> getObject(String columnLabel) {
        Logger.trace("getObject");
        try { return Result.success(delegate.getObject(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> findColumn(String columnLabel) {
        Logger.trace("findColumn");
        try { return Result.success(delegate.findColumn(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Reader, String> getCharacterStream(int columnIndex) {
        try { return Result.success(delegate.getCharacterStream(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Reader, String> getCharacterStream(String columnLabel) {
        try { return Result.success(delegate.getCharacterStream(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isBeforeFirst() {
        Logger.trace("isBeforeFirst");
        try { return Result.success(delegate.isBeforeFirst()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isAfterLast() {
        Logger.trace("isAfterLast");
        try { return Result.success(delegate.isAfterLast()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isFirst() {
        Logger.trace("isFirst");
        try { return Result.success(delegate.isFirst()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isLast() {
        Logger.trace("isLast");
        try { return Result.success(delegate.isLast()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> beforeFirst() {
        Logger.trace("beforeFirst");
        try { delegate.beforeFirst(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> afterLast() {
        Logger.trace("afterLast");
        try { delegate.afterLast(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> first() {
        Logger.trace("first");
        try { return Result.success(delegate.first()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> last() {
        Logger.trace("last");
        try { return Result.success(delegate.last()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getRow() {
        Logger.trace("getRow");
        try { return Result.success(delegate.getRow()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> absolute(int row) {
        Logger.trace("absolute");
        try { return Result.success(delegate.absolute(row)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> relative(int rows) {
        Logger.trace("relative");
        try { return Result.success(delegate.relative(rows)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> previous() {
        Logger.trace("previous");
        try { return Result.success(delegate.previous()); }
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

    public Result<Integer, String> getType() {
        try { return Result.success(delegate.getType()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getConcurrency() {
        try { return Result.success(delegate.getConcurrency()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> rowUpdated() {
        Logger.trace("rowUpdated");
        try { return Result.success(delegate.rowUpdated()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> rowInserted() {
        Logger.trace("rowInserted");
        try { return Result.success(delegate.rowInserted()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> rowDeleted() {
        Logger.trace("rowDeleted");
        try { return Result.success(delegate.rowDeleted()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateNull(int columnIndex) {
        try { delegate.updateNull(columnIndex); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateBoolean(int columnIndex, boolean x) {
        try { delegate.updateBoolean(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateByte(int columnIndex, byte x) {
        try { delegate.updateByte(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateShort(int columnIndex, short x) {
        try { delegate.updateShort(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateInt(int columnIndex, int x) {
        try { delegate.updateInt(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateLong(int columnIndex, long x) {
        try { delegate.updateLong(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateFloat(int columnIndex, float x) {
        try { delegate.updateFloat(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateDouble(int columnIndex, double x) {
        try { delegate.updateDouble(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateBigDecimal(int columnIndex, BigDecimal x) {
        try { delegate.updateBigDecimal(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateString(int columnIndex, String x) {
        try { delegate.updateString(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateBytes(int columnIndex, byte[] x) {
        try { delegate.updateBytes(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateDate(int columnIndex, Date x) {
        try { delegate.updateDate(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateTime(int columnIndex, Time x) {
        try { delegate.updateTime(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateTimestamp(int columnIndex, Timestamp x) {
        try { delegate.updateTimestamp(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateAsciiStream(int columnIndex, InputStream x, int length) {
        try { delegate.updateAsciiStream(columnIndex, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateBinaryStream(int columnIndex, InputStream x, int length) {
        try { delegate.updateBinaryStream(columnIndex, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateCharacterStream(int columnIndex, Reader x, int length) {
        try { delegate.updateCharacterStream(columnIndex, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateObject(int columnIndex, Object x, int scaleOrLength) {
        try { delegate.updateObject(columnIndex, x, scaleOrLength); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateObject(int columnIndex, Object x) {
        try { delegate.updateObject(columnIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> insertRow() {
        Logger.trace("insertRow");
        try { delegate.insertRow(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> updateRow() {
        Logger.trace("updateRow");
        try { delegate.updateRow(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> deleteRow() {
        Logger.trace("deleteRow");
        try { delegate.deleteRow(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> refreshRow() {
        Logger.trace("refreshRow");
        try { delegate.refreshRow(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> cancelRowUpdates() {
        Logger.trace("cancelRowUpdates");
        try { delegate.cancelRowUpdates(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> moveToInsertRow() {
        Logger.trace("moveToInsertRow");
        try { delegate.moveToInsertRow(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> moveToCurrentRow() {
        Logger.trace("moveToCurrentRow");
        try { delegate.moveToCurrentRow(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCStatement, String> getStatement() {
        try { return Result.success(new JDBCStatement(delegate.getStatement())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Ref, String> getRef(int columnIndex) {
        try { return Result.success(delegate.getRef(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Blob, String> getBlob(int columnIndex) {
        try { return Result.success(delegate.getBlob(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Clob, String> getClob(int columnIndex) {
        try { return Result.success(delegate.getClob(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Array, String> getArray(int columnIndex) {
        try { return Result.success(delegate.getArray(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Date, String> getDate(int columnIndex, Calendar cal) {
        try { return Result.success(delegate.getDate(columnIndex, cal)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Time, String> getTime(int columnIndex, Calendar cal) {
        try { return Result.success(delegate.getTime(columnIndex, cal)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Timestamp, String> getTimestamp(int columnIndex, Calendar cal) {
        try { return Result.success(delegate.getTimestamp(columnIndex, cal)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<URL, String> getURL(int columnIndex) {
        try { return Result.success(delegate.getURL(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<RowId, String> getRowId(int columnIndex) {
        try { return Result.success(delegate.getRowId(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getHoldability() {
        try { return Result.success(delegate.getHoldability()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isClosed() {
        Logger.trace("isClosed");
        try { return Result.success(delegate.isClosed()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getNString(int columnIndex) {
        try { return Result.success(delegate.getNString(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getNString(String columnLabel) {
        try { return Result.success(delegate.getNString(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Reader, String> getNCharacterStream(int columnIndex) {
        try { return Result.success(delegate.getNCharacterStream(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Reader, String> getNCharacterStream(String columnLabel) {
        try { return Result.success(delegate.getNCharacterStream(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<NClob, String> getNClob(int columnIndex) {
        try { return Result.success(delegate.getNClob(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<NClob, String> getNClob(String columnLabel) {
        try { return Result.success(delegate.getNClob(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLXML, String> getSQLXML(int columnIndex) {
        try { return Result.success(delegate.getSQLXML(columnIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLXML, String> getSQLXML(String columnLabel) {
        try { return Result.success(delegate.getSQLXML(columnLabel)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public <T> Result<T, String> getObject(int columnIndex, Class<T> type) {
        try { return Result.success(delegate.getObject(columnIndex, type)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public <T> Result<T, String> getObject(String columnLabel, Class<T> type) {
        try { return Result.success(delegate.getObject(columnLabel, type)); }
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

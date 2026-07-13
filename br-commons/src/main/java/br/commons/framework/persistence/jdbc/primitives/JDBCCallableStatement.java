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
import java.util.Arrays;
import java.util.Calendar;

@NullMarked
public record JDBCCallableStatement(
        CallableStatement delegate
) {

    public Result<Void, String> registerOutParameter(int parameterIndex, int sqlType) {
        Logger.trace("registerOutParameter(%s, %s)", parameterIndex, Logger.lazy(() -> JDBCConstants.sqlType(sqlType)));
        try { delegate.registerOutParameter(parameterIndex, sqlType); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> registerOutParameter(int parameterIndex, int sqlType, int scale) {
        Logger.trace("registerOutParameter(%s, %s, %s)", parameterIndex, Logger.lazy(() -> JDBCConstants.sqlType(sqlType)), scale);
        try { delegate.registerOutParameter(parameterIndex, sqlType, scale); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> wasNull() {
        try { return Result.success(delegate.wasNull()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getString(int parameterIndex) {
        try { return Result.success(delegate.getString(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getBoolean(int parameterIndex) {
        try { return Result.success(delegate.getBoolean(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Byte, String> getByte(int parameterIndex) {
        try { return Result.success(delegate.getByte(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Short, String> getShort(int parameterIndex) {
        try { return Result.success(delegate.getShort(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getInt(int parameterIndex) {
        try { return Result.success(delegate.getInt(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Long, String> getLong(int parameterIndex) {
        try { return Result.success(delegate.getLong(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Float, String> getFloat(int parameterIndex) {
        try { return Result.success(delegate.getFloat(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Double, String> getDouble(int parameterIndex) {
        try { return Result.success(delegate.getDouble(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<BigDecimal, String> getBigDecimal(int parameterIndex) {
        try { return Result.success(delegate.getBigDecimal(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<byte[], String> getBytes(int parameterIndex) {
        try { return Result.success(delegate.getBytes(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Date, String> getDate(int parameterIndex) {
        try { return Result.success(delegate.getDate(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Time, String> getTime(int parameterIndex) {
        try { return Result.success(delegate.getTime(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Timestamp, String> getTimestamp(int parameterIndex) {
        try { return Result.success(delegate.getTimestamp(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Object, String> getObject(int parameterIndex) {
        try { return Result.success(delegate.getObject(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> registerOutParameter(int parameterIndex, int sqlType, String typeName) {
        Logger.trace("registerOutParameter(%s, %s, %s)", parameterIndex, Logger.lazy(() -> JDBCConstants.sqlType(sqlType)), typeName);
        try { delegate.registerOutParameter(parameterIndex, sqlType, typeName); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> registerOutParameter(String parameterName, int sqlType) {
        Logger.trace("registerOutParameter(%s, %s)", parameterName, Logger.lazy(() -> JDBCConstants.sqlType(sqlType)));
        try { delegate.registerOutParameter(parameterName, sqlType); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> registerOutParameter(String parameterName, int sqlType, int scale) {
        Logger.trace("registerOutParameter(%s, %s, %s)", parameterName, Logger.lazy(() -> JDBCConstants.sqlType(sqlType)), scale);
        try { delegate.registerOutParameter(parameterName, sqlType, scale); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<URL, String> getURL(int parameterIndex) {
        try { return Result.success(delegate.getURL(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setURL(String parameterName, URL val) {
        Logger.trace("setURL(%s, %s)", parameterName, val);
        try { delegate.setURL(parameterName, val); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNull(String parameterName, int sqlType) {
        Logger.trace("setNull(%s, %s)", parameterName, Logger.lazy(() -> JDBCConstants.sqlType(sqlType)));
        try { delegate.setNull(parameterName, sqlType); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBoolean(String parameterName, boolean x) {
        Logger.trace("setBoolean(%s, %s)", parameterName, x);
        try { delegate.setBoolean(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setByte(String parameterName, byte x) {
        Logger.trace("setByte(%s, %s)", parameterName, x);
        try { delegate.setByte(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setShort(String parameterName, short x) {
        Logger.trace("setShort(%s, %s)", parameterName, x);
        try { delegate.setShort(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setInt(String parameterName, int x) {
        Logger.trace("setInt(%s, %s)", parameterName, x);
        try { delegate.setInt(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setLong(String parameterName, long x) {
        Logger.trace("setLong(%s, %s)", parameterName, x);
        try { delegate.setLong(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setFloat(String parameterName, float x) {
        Logger.trace("setFloat(%s, %s)", parameterName, x);
        try { delegate.setFloat(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setDouble(String parameterName, double x) {
        Logger.trace("setDouble(%s, %s)", parameterName, x);
        try { delegate.setDouble(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBigDecimal(String parameterName, BigDecimal x) {
        Logger.trace("setBigDecimal(%s, %s)", parameterName, x);
        try { delegate.setBigDecimal(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setString(String parameterName, String x) {
        Logger.trace("setString(%s, %s)", parameterName, x);
        try { delegate.setString(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBytes(String parameterName, byte[] x) {
        Logger.trace("setBytes(%s, %s)", parameterName, Logger.lazy(() -> Arrays.toString(x)));
        try { delegate.setBytes(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setDate(String parameterName, Date x) {
        Logger.trace("setDate(%s, %s)", parameterName, x);
        try { delegate.setDate(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTime(String parameterName, Time x) {
        Logger.trace("setTime(%s, %s)", parameterName, x);
        try { delegate.setTime(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTimestamp(String parameterName, Timestamp x) {
        Logger.trace("setTimestamp(%s, %s)", parameterName, x);
        try { delegate.setTimestamp(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setAsciiStream(String parameterName, InputStream x, int length) {
        Logger.trace("setAsciiStream(%s, %s, %s)", parameterName, x, length);
        try { delegate.setAsciiStream(parameterName, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBinaryStream(String parameterName, InputStream x, int length) {
        Logger.trace("setBinaryStream(%s, %s, %s)", parameterName, x, length);
        try { delegate.setBinaryStream(parameterName, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setObject(String parameterName, Object x, int targetSqlType, int scale) {
        Logger.trace("setObject(%s, %s, %s, %s)", parameterName, x, Logger.lazy(() -> JDBCConstants.sqlType(targetSqlType)), scale);
        try { delegate.setObject(parameterName, x, targetSqlType, scale); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setObject(String parameterName, Object x, int targetSqlType) {
        Logger.trace("setObject(%s, %s, %s)", parameterName, x, Logger.lazy(() -> JDBCConstants.sqlType(targetSqlType)));
        try { delegate.setObject(parameterName, x, targetSqlType); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setObject(String parameterName, Object x) {
        Logger.trace("setObject(%s, %s)", parameterName, x);
        try { delegate.setObject(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCharacterStream(String parameterName, Reader reader, int length) {
        Logger.trace("setCharacterStream(%s, %s, %s)", parameterName, reader, length);
        try { delegate.setCharacterStream(parameterName, reader, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setDate(String parameterName, Date x, Calendar cal) {
        Logger.trace("setDate(%s, %s, %s)", parameterName, x, cal);
        try { delegate.setDate(parameterName, x, cal); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTime(String parameterName, Time x, Calendar cal) {
        Logger.trace("setTime(%s, %s, %s)", parameterName, x, cal);
        try { delegate.setTime(parameterName, x, cal); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTimestamp(String parameterName, Timestamp x, Calendar cal) {
        Logger.trace("setTimestamp(%s, %s, %s)", parameterName, x, cal);
        try { delegate.setTimestamp(parameterName, x, cal); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNull(String parameterName, int sqlType, String typeName) {
        Logger.trace("setNull(%s, %s, %s)", parameterName, Logger.lazy(() -> JDBCConstants.sqlType(sqlType)), typeName);
        try { delegate.setNull(parameterName, sqlType, typeName); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getString(String parameterName) {
        try { return Result.success(delegate.getString(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getBoolean(String parameterName) {
        try { return Result.success(delegate.getBoolean(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Byte, String> getByte(String parameterName) {
        try { return Result.success(delegate.getByte(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Short, String> getShort(String parameterName) {
        try { return Result.success(delegate.getShort(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getInt(String parameterName) {
        try { return Result.success(delegate.getInt(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Long, String> getLong(String parameterName) {
        try { return Result.success(delegate.getLong(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Float, String> getFloat(String parameterName) {
        try { return Result.success(delegate.getFloat(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Double, String> getDouble(String parameterName) {
        try { return Result.success(delegate.getDouble(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<byte[], String> getBytes(String parameterName) {
        try { return Result.success(delegate.getBytes(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Date, String> getDate(String parameterName) {
        try { return Result.success(delegate.getDate(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Time, String> getTime(String parameterName) {
        try { return Result.success(delegate.getTime(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Timestamp, String> getTimestamp(String parameterName) {
        try { return Result.success(delegate.getTimestamp(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Object, String> getObject(String parameterName) {
        try { return Result.success(delegate.getObject(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<BigDecimal, String> getBigDecimal(String parameterName) {
        try { return Result.success(delegate.getBigDecimal(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Ref, String> getRef(int parameterIndex) {
        try { return Result.success(delegate.getRef(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Blob, String> getBlob(int parameterIndex) {
        try { return Result.success(delegate.getBlob(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Clob, String> getClob(int parameterIndex) {
        try { return Result.success(delegate.getClob(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Array, String> getArray(int parameterIndex) {
        try { return Result.success(delegate.getArray(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Date, String> getDate(int parameterIndex, Calendar cal) {
        try { return Result.success(delegate.getDate(parameterIndex, cal)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Time, String> getTime(int parameterIndex, Calendar cal) {
        try { return Result.success(delegate.getTime(parameterIndex, cal)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Timestamp, String> getTimestamp(int parameterIndex, Calendar cal) {
        try { return Result.success(delegate.getTimestamp(parameterIndex, cal)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<URL, String> getURL(String parameterName) {
        try { return Result.success(delegate.getURL(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<RowId, String> getRowId(int parameterIndex) {
        try { return Result.success(delegate.getRowId(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<RowId, String> getRowId(String parameterName) {
        try { return Result.success(delegate.getRowId(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setRowId(String parameterName, RowId x) {
        Logger.trace("setRowId(%s, %s)", parameterName, x);
        try { delegate.setRowId(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNString(String parameterName, String value) {
        Logger.trace("setNString(%s, %s)", parameterName, value);
        try { delegate.setNString(parameterName, value); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNCharacterStream(String parameterName, Reader value, long length) {
        Logger.trace("setNCharacterStream(%s, %s, %s)", parameterName, value, length);
        try { delegate.setNCharacterStream(parameterName, value, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNClob(String parameterName, NClob value) {
        Logger.trace("setNClob(%s, %s)", parameterName, value);
        try { delegate.setNClob(parameterName, value); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClob(String parameterName, Reader reader, long length) {
        Logger.trace("setClob(%s, %s, %s)", parameterName, reader, length);
        try { delegate.setClob(parameterName, reader, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBlob(String parameterName, InputStream inputStream, long length) {
        Logger.trace("setBlob(%s, %s, %s)", parameterName, inputStream, length);
        try { delegate.setBlob(parameterName, inputStream, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNClob(String parameterName, Reader reader, long length) {
        Logger.trace("setNClob(%s, %s, %s)", parameterName, reader, length);
        try { delegate.setNClob(parameterName, reader, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<NClob, String> getNClob(int parameterIndex) {
        try { return Result.success(delegate.getNClob(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<NClob, String> getNClob(String parameterName) {
        try { return Result.success(delegate.getNClob(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setSQLXML(String parameterName, SQLXML xmlObject) {
        Logger.trace("setSQLXML(%s, %s)", parameterName, xmlObject);
        try { delegate.setSQLXML(parameterName, xmlObject); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLXML, String> getSQLXML(int parameterIndex) {
        try { return Result.success(delegate.getSQLXML(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<SQLXML, String> getSQLXML(String parameterName) {
        try { return Result.success(delegate.getSQLXML(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getNString(int parameterIndex) {
        try { return Result.success(delegate.getNString(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<String, String> getNString(String parameterName) {
        try { return Result.success(delegate.getNString(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Reader, String> getNCharacterStream(int parameterIndex) {
        try { return Result.success(delegate.getNCharacterStream(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Reader, String> getNCharacterStream(String parameterName) {
        try { return Result.success(delegate.getNCharacterStream(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Reader, String> getCharacterStream(int parameterIndex) {
        try { return Result.success(delegate.getCharacterStream(parameterIndex)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Reader, String> getCharacterStream(String parameterName) {
        try { return Result.success(delegate.getCharacterStream(parameterName)); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBlob(String parameterName, Blob x) {
        Logger.trace("setBlob(%s, %s)", parameterName, x);
        try { delegate.setBlob(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClob(String parameterName, Clob x) {
        Logger.trace("setClob(%s, %s)", parameterName, x);
        try { delegate.setClob(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setAsciiStream(String parameterName, InputStream x, long length) {
        Logger.trace("setAsciiStream(%s, %s, %s)", parameterName, x, length);
        try { delegate.setAsciiStream(parameterName, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBinaryStream(String parameterName, InputStream x, long length) {
        Logger.trace("setBinaryStream(%s, %s, %s)", parameterName, x, length);
        try { delegate.setBinaryStream(parameterName, x, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCharacterStream(String parameterName, Reader reader, long length) {
        Logger.trace("setCharacterStream(%s, %s, %s)", parameterName, reader, length);
        try { delegate.setCharacterStream(parameterName, reader, length); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setAsciiStream(String parameterName, InputStream x) {
        Logger.trace("setAsciiStream(%s, %s)", parameterName, x);
        try { delegate.setAsciiStream(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBinaryStream(String parameterName, InputStream x) {
        Logger.trace("setBinaryStream(%s, %s)", parameterName, x);
        try { delegate.setBinaryStream(parameterName, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setCharacterStream(String parameterName, Reader reader) {
        Logger.trace("setCharacterStream(%s, %s)", parameterName, reader);
        try { delegate.setCharacterStream(parameterName, reader); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNCharacterStream(String parameterName, Reader value) {
        Logger.trace("setNCharacterStream(%s, %s)", parameterName, value);
        try { delegate.setNCharacterStream(parameterName, value); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setClob(String parameterName, Reader reader) {
        Logger.trace("setClob(%s, %s)", parameterName, reader);
        try { delegate.setClob(parameterName, reader); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBlob(String parameterName, InputStream inputStream) {
        Logger.trace("setBlob(%s, %s)", parameterName, inputStream);
        try { delegate.setBlob(parameterName, inputStream); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNClob(String parameterName, Reader reader) {
        Logger.trace("setNClob(%s, %s)", parameterName, reader);
        try { delegate.setNClob(parameterName, reader); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> executeQuery() {
        Logger.trace("executeQuery()");
        try { return Result.success(new JDBCResultSet(delegate.executeQuery())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> executeUpdate() {
        Logger.trace("executeUpdate()");
        try { return Result.success(delegate.executeUpdate()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setNull(int parameterIndex, int sqlType) {
        Logger.trace("setNull(%s, %s)", parameterIndex, Logger.lazy(() -> JDBCConstants.sqlType(sqlType)));
        try { delegate.setNull(parameterIndex, sqlType); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBoolean(int parameterIndex, boolean x) {
        Logger.trace("setBoolean(%s, %s)", parameterIndex, x);
        try { delegate.setBoolean(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setByte(int parameterIndex, byte x) {
        Logger.trace("setByte(%s, %s)", parameterIndex, x);
        try { delegate.setByte(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setShort(int parameterIndex, short x) {
        Logger.trace("setShort(%s, %s)", parameterIndex, x);
        try { delegate.setShort(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setInt(int parameterIndex, int x) {
        Logger.trace("setInt(%s, %s)", parameterIndex, x);
        try { delegate.setInt(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setLong(int parameterIndex, long x) {
        Logger.trace("setLong(%s, %s)", parameterIndex, x);
        try { delegate.setLong(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setFloat(int parameterIndex, float x) {
        Logger.trace("setFloat(%s, %s)", parameterIndex, x);
        try { delegate.setFloat(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setDouble(int parameterIndex, double x) {
        Logger.trace("setDouble(%s, %s)", parameterIndex, x);
        try { delegate.setDouble(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBigDecimal(int parameterIndex, BigDecimal x) {
        Logger.trace("setBigDecimal(%s, %s)", parameterIndex, x);
        try { delegate.setBigDecimal(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setString(int parameterIndex, String x) {
        Logger.trace("setString(%s, %s)", parameterIndex, x);
        try { delegate.setString(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setBytes(int parameterIndex, byte[] x) {
        Logger.trace("setBytes(%s, %s)", parameterIndex, Logger.lazy(() -> Arrays.toString(x)));
        try { delegate.setBytes(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setDate(int parameterIndex, Date x) {
        Logger.trace("setDate(%s, %s)", parameterIndex, x);
        try { delegate.setDate(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTime(int parameterIndex, Time x) {
        Logger.trace("setTime(%s, %s)", parameterIndex, x);
        try { delegate.setTime(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setTimestamp(int parameterIndex, Timestamp x) {
        Logger.trace("setTimestamp(%s, %s)", parameterIndex, x);
        try { delegate.setTimestamp(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> clearParameters() {
        Logger.trace("clearParameters()");
        try { delegate.clearParameters(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setObject(int parameterIndex, Object x, int targetSqlType) {
        Logger.trace("setObject(%s, %s, %s)", parameterIndex, x, Logger.lazy(() -> JDBCConstants.sqlType(targetSqlType)));
        try { delegate.setObject(parameterIndex, x, targetSqlType); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setObject(int parameterIndex, Object x) {
        Logger.trace("setObject(%s, %s)", parameterIndex, x);
        try { delegate.setObject(parameterIndex, x); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> execute() {
        try { return Result.success(delegate.execute()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> addBatch() {
        try { delegate.addBatch(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> close() {
        try { delegate.close(); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getResultSet() {
        try { return Result.success(new JDBCResultSet(delegate.getResultSet())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getUpdateCount() {
        try { return Result.success(delegate.getUpdateCount()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> getMoreResults() {
        try { return Result.success(delegate.getMoreResults()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCConnection, String> getConnection() {
        try { return Result.success(new JDBCConnection(delegate.getConnection())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<JDBCResultSet, String> getGeneratedKeys() {
        try { return Result.success(new JDBCResultSet(delegate.getGeneratedKeys())); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxFieldSize() {
        try { return Result.success(delegate.getMaxFieldSize()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setMaxFieldSize(int max) {
        try { delegate.setMaxFieldSize(max); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getMaxRows() {
        try { return Result.success(delegate.getMaxRows()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setMaxRows(int max) {
        try { delegate.setMaxRows(max); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Integer, String> getQueryTimeout() {
        try { return Result.success(delegate.getQueryTimeout()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setQueryTimeout(int seconds) {
        try { delegate.setQueryTimeout(seconds); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> cancel() {
        try { delegate.cancel(); return Result.success(); }
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

    public Result<Integer, String> getFetchSize() {
        try { return Result.success(delegate.getFetchSize()); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Void, String> setFetchSize(int rows) {
        try { delegate.setFetchSize(rows); return Result.success(); }
        catch (SQLException ex) { return Result.failure(Strings.orEmpty(ex.getMessage())); }
    }

    public Result<Boolean, String> isClosed() {
        try { return Result.success(delegate.isClosed()); }
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

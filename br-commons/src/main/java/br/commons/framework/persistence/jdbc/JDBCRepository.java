package br.commons.framework.persistence.jdbc;

import br.commons.Logger;
import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.primitives.JDBCMetaData;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base de repositório JDBC para uma única tabela. O construtor consulta o
 * {@link JDBCMetaData} (DatabaseMetaData) da conexão para descobrir as colunas e
 * a chave primária da tabela, e com isso fornece operações de CRUD genéricas
 * ({@link #findAll()}, {@link #findById(Object...)}, {@link #save(Object)},
 * {@link #deleteById(Object...)}, {@link #count()}, {@link #exists(Object...)}).
 *
 * <p>Subtipos implementam apenas o mapeamento entre linha e entidade:
 * {@link #map(JDBCResultSet)} e {@link #values(Object)}. Colunas que não devem ser
 * sobrescritas em {@code UPDATE} (ex.: {@code TMS_CREATE_AT}) são declaradas em
 * {@link #updateImmutableColumns()}.
 *
 * @param <T> tipo da entidade persistida nesta tabela
 */
@NullMarked
public abstract class JDBCRepository<T> {

    protected final DataSource datasource = Registry.get(DataSource.class);

    private final @Nullable String schema;
    public @Nullable String schema() { return schema; }

    private final String table;
    public String table() { return table; }

    private final List<String> columns = new ArrayList<>();
    public List<String> columns() { return columns; }

    private final List<String> primaryKeys = new ArrayList<>();
    public List<String> primaryKeys() { return primaryKeys; }

    protected JDBCRepository(String table) {
        this(null, table);
    }

    protected JDBCRepository(@Nullable String schema, String table) {
        this.schema = schema;
        this.table = table;
        introspect();
    }

    // ----------------------------------------------------------------------
    // Hooks do subtipo
    // ----------------------------------------------------------------------

    /** Mapeia a linha corrente do ResultSet para a entidade. */
    protected abstract T map(JDBCResultSet rs);

    /** Valores da entidade por coluna (já convertidos: UUID#toString, enum#name, "Y"/"N", Timestamp/Date). */
    protected abstract Map<String, @Nullable Object> values(T entity);

    /** Colunas preservadas em UPDATE (ex.: {@code Set.of("TMS_CREATE_AT")}). Vazio por padrão. */
    protected Set<String> updateImmutableColumns() {
        return Set.of();
    }

    // ----------------------------------------------------------------------
    // CRUD genérico
    // ----------------------------------------------------------------------

    public List<T> findAll() {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table,
                this::mapList
        );
    }

    public Optional<T> findById(Object... pk) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table + " WHERE " + pkWhere(),
                JDBCParameter.of(pk),
                this::mapList
        ).stream().findFirst();
    }

    public boolean exists(Object... pk) {
        return datasource.query(
                "SELECT 1 FROM " + table + " WHERE " + pkWhere(),
                JDBCParameter.of(pk),
                rs -> rs.next().get()
        );
    }

    public long count() {
        return datasource.query(
                "SELECT COUNT(*) FROM " + table,
                rs -> { rs.next().get(); return rs.getLong(1).get(); }
        );
    }

    public T insert(T entity) {
        val vals = values(entity);
        datasource.execute(insertSql(), insertParams(vals));
        return entity;
    }

    public T update(T entity) {
        val vals = values(entity);
        datasource.execute(updateSql(), updateParams(vals));
        return entity;
    }

    /** Insere ou atualiza a entidade de forma atômica (verifica existência e grava na mesma transação). */
    public T save(T entity) {
        val vals = values(entity);
        val pk = pkValues(vals);

        datasource.transaction(tx -> {
            val exists = tx.query(
                    "SELECT 1 FROM " + table + " WHERE " + pkWhere(),
                    JDBCParameter.of(pk),
                    rs -> rs.next().get()
            ).get();

            if (exists) tx.execute(updateSql(), updateParams(vals)).get();
            else tx.execute(insertSql(), insertParams(vals)).get();

            return Result.success(true);
        });
        return entity;
    }

    public void deleteById(Object... pk) {
        datasource.execute(
                "DELETE FROM " + table + " WHERE " + pkWhere(),
                JDBCParameter.of(pk)
        );
    }

    // ----------------------------------------------------------------------
    // SQL + parâmetros
    // ----------------------------------------------------------------------

    protected String columnList() {
        return String.join(", ", columns);
    }

    private String pkWhere() {
        return primaryKeys.stream().map(c -> c + " = ?").collect(Collectors.joining(" AND "));
    }

    private List<String> updatableColumns() {
        val immutable = updateImmutableColumns();
        return columns.stream()
                .filter(c -> !primaryKeys.contains(c))
                .filter(c -> !immutable.contains(c))
                .toList();
    }

    private String insertSql() {
        val placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        return "INSERT INTO " + table + " (" + columnList() + ") VALUES (" + placeholders + ")";
    }

    private String updateSql() {
        val set = updatableColumns().stream().map(c -> c + " = ?").collect(Collectors.joining(", "));
        return "UPDATE " + table + " SET " + set + " WHERE " + pkWhere();
    }

    private List<JDBCParameter> insertParams(Map<String, @Nullable Object> vals) {
        return JDBCParameter.of(columns.stream().map(vals::get).toArray());
    }

    private List<JDBCParameter> updateParams(Map<String, @Nullable Object> vals) {
        val ordered = new ArrayList<@Nullable Object>();
        updatableColumns().forEach(c -> ordered.add(vals.get(c)));
        primaryKeys.forEach(c -> ordered.add(vals.get(c)));
        return JDBCParameter.of(ordered.toArray());
    }

    private Object[] pkValues(Map<String, @Nullable Object> vals) {
        return primaryKeys.stream().map(vals::get).toArray();
    }

    protected List<T> mapList(JDBCResultSet rs) {
        val list = new ArrayList<T>();
        while (rs.next().get()) list.add(map(rs));
        return list;
    }

    // ----------------------------------------------------------------------
    // Introspecção do metadata
    // ----------------------------------------------------------------------

    private void introspect() {
        val schemaPattern = (schema == null || schema.isBlank()) ? null : schema;

        datasource.begin()
                .flatMap(tx -> tx.execute(conn -> conn.getMetaData().map(md -> readMetaData(md, schemaPattern))))
                .ifFailure(error -> Logger.fatal("Failed to introspect table %s: %s", table, error))
                .get();

        if (columns.isEmpty()) {
            Logger.fatal("Table %s has no columns (not found in metadata)", table);
        }
    }

    private Boolean readMetaData(JDBCMetaData md, @Nullable String schemaPattern) {
        val columnsRs = md.getColumns(null, schemaPattern, table, null).get();
        while (columnsRs.next().get()) {
            columns.add(columnsRs.getString("COLUMN_NAME").get());
        }

        val pkBySeq = new TreeMap<Integer, String>();
        val pkRs = md.getPrimaryKeys(null, schemaPattern, table).get();
        while (pkRs.next().get()) {
            pkBySeq.put(pkRs.getInt("KEY_SEQ").get(), pkRs.getString("COLUMN_NAME").get());
        }
        primaryKeys.addAll(pkBySeq.values());

        return true;
    }
}

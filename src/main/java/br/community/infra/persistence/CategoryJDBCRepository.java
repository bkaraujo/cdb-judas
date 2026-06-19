package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Category;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._0_domain.repository.CategoryRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link CategoryRepository}; tabela {@code MON_CATEGORY}. */
@NullMarked
public final class CategoryJDBCRepository implements CategoryRepository {

    private static final String COLUMNS = "ID, TXT_NATURE, TXT_NAME, COD_PARENT, BOL_SYSTEM";

    private final DataSource dataSource;

    public CategoryJDBCRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Category> findAll() {
        return dataSource.executeQuery("SELECT " + COLUMNS + " FROM MON_CATEGORY", this::toCategories).getOrThrow();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM MON_CATEGORY WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toCategories
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public Category save(Category entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val parent = entity.parentId();
        @Nullable String parentStr = parent == null ? null : parent.toString();
        val systemFlag = entity.isSystem() ? "Y" : "N";
        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_CATEGORY (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.nature().name()),
                    new JDBCPreparedParameter(3, entity.name()),
                    new JDBCPreparedParameter(4, parentStr),
                    new JDBCPreparedParameter(5, systemFlag)
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE MON_CATEGORY SET TXT_NATURE = ?, TXT_NAME = ?, COD_PARENT = ?, BOL_SYSTEM = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.nature().name()),
                    new JDBCPreparedParameter(2, entity.name()),
                    new JDBCPreparedParameter(3, parentStr),
                    new JDBCPreparedParameter(4, systemFlag),
                    new JDBCPreparedParameter(5, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM MON_CATEGORY WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Category> toCategories(JDBCResultSet rs) {
        val categories = new ArrayList<Category>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) categories.add(toCategory(rs));
        return categories;
    }

    private Category toCategory(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").getOrThrow());
        val nature = Transaction.Type.valueOf(rs.getString("TXT_NATURE").getOrThrow());
        val name = rs.getString("TXT_NAME").getOrThrow();
        final boolean isSystem = "Y".equals(rs.getString("BOL_SYSTEM").getOrThrow());

        @Nullable String parentRaw = rs.getString("COD_PARENT").getOrThrow();
        @Nullable UUID parentId = (parentRaw == null || parentRaw.isBlank()) ? null : UUID.fromString(parentRaw);

        return new Category(id, nature, name, parentId, isSystem);
    }
}

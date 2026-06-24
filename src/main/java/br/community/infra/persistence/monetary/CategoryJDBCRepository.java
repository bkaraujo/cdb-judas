package br.community.infra.persistence.monetary;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
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

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<Category> findAll() {
        return dataSource.query("SELECT " + COLUMNS + " FROM MON_CATEGORY", this::toCategories);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM MON_CATEGORY WHERE ID = ?",
                JDBCParameter.of(id.toString()),
                this::toCategories
        ).stream().findFirst();
    }

    @Override
    public Category save(Category entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val parent = entity.parentId();
        val parentStr = parent == null ? null : parent.toString();
        val systemFlag = entity.isSystem() ? "Y" : "N";
        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO MON_CATEGORY (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
                    JDBCParameter.of (
                            entity.id().toString(),
                            entity.nature().name(),
                            entity.name(),
                            parentStr,
                            systemFlag
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE MON_CATEGORY SET TXT_NATURE = ?, TXT_NAME = ?, COD_PARENT = ?, BOL_SYSTEM = ? WHERE ID = ?",
                    JDBCParameter.of (
                            entity.nature().name(),
                            entity.name(),
                            parentStr,
                            systemFlag,
                            entity.id().toString()
                    )
            );
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM MON_CATEGORY WHERE ID = ?", JDBCParameter.of(id.toString()));
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    private List<Category> toCategories(JDBCResultSet rs) {
        val categories = new ArrayList<Category>();
        while (rs.next().get()) categories.add(toCategory(rs));
        return categories;
    }

    private Category toCategory(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val nature = Transaction.Type.valueOf(rs.getString("TXT_NATURE").get());
        val name = rs.getString("TXT_NAME").get();
        val isSystem = "Y".equals(rs.getString("BOL_SYSTEM").get());

        final @Nullable String parentRaw = rs.getString("COD_PARENT").get();
        final @Nullable UUID parentId = (parentRaw == null || parentRaw.isBlank()) ? null : UUID.fromString(parentRaw);

        return new Category(id, nature, name, parentId, isSystem);
    }
}

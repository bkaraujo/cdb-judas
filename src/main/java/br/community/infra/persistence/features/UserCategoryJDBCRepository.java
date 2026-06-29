package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.feature.user.categories.UserCategory;
import br.community.feature.user.categories.UserCategoryRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link UserCategoryRepository}; tabela {@code USER_CATEGORY}. */
@NullMarked
public final class UserCategoryJDBCRepository implements UserCategoryRepository {

    private static final String COLUMNS = "ID, COD_USER, TXT_NATURE, TXT_NAME, COD_PARENT, BOL_SYSTEM, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<UserCategory> findAllByUser(UUID userId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_CATEGORY WHERE COD_USER = ?",
                JDBCParameter.of(userId.toString()),
                this::toCategories
        );
    }

    @Override
    public Optional<UserCategory> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_CATEGORY WHERE ID = ?",
                JDBCParameter.of(id.toString()),
                this::toCategories
        ).stream().findFirst();
    }

    @Override
    public UserCategory save(UserCategory entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val parentStr = entity.parentId() == null ? null : entity.parentId().toString();
        val systemFlag = entity.isSystem() ? "Y" : "N";
        val activeFlag = entity.active() ? "Y" : "N";
        val now = Timestamp.valueOf(Time.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO USER_CATEGORY (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    JDBCParameter.of (
                            entity.id().toString(),
                            entity.userId().toString(),
                            entity.nature().name(),
                            entity.name(),
                            parentStr,
                            systemFlag,
                            activeFlag,
                            now,
                            now
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE USER_CATEGORY SET TXT_NATURE = ?, TXT_NAME = ?, COD_PARENT = ?, BOL_SYSTEM = ?, FLG_ACTIVE = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    JDBCParameter.of (
                            entity.nature().name(),
                            entity.name(),
                            parentStr,
                            systemFlag,
                            activeFlag,
                            now,
                            entity.id().toString()
                    )
            );
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM USER_CATEGORY WHERE ID = ?",
                JDBCParameter.of(id.toString()));
    }

    private List<UserCategory> toCategories(JDBCResultSet rs) {
        val categories = new ArrayList<UserCategory>();
        while (rs.next().get()) categories.add(toCategory(rs));
        return categories;
    }

    private UserCategory toCategory(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val userId = UUID.fromString(rs.getString("COD_USER").get());
        val nature = Transaction.Type.valueOf(rs.getString("TXT_NATURE").get());
        val name = rs.getString("TXT_NAME").get();
        val isSystem = "Y".equals(rs.getString("BOL_SYSTEM").get());
        val active = "Y".equals(rs.getString("FLG_ACTIVE").get());

        final @Nullable String parentRaw = rs.getString("COD_PARENT").get();
        final @Nullable UUID parentId = (parentRaw == null || parentRaw.isBlank()) ? null : UUID.fromString(parentRaw);

        val createRaw = rs.getTimestamp("TMS_CREATE_AT").get();
        val createdAt = createRaw.toLocalDateTime();
        val updateRaw = rs.getTimestamp("TMS_UPDATED_AT").get();
        val updatedAt = updateRaw.toLocalDateTime();

        return new UserCategory(id, userId, nature, name, parentId, isSystem, active, createdAt, updatedAt);
    }
}

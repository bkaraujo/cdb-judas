package br.community.infra.persistence;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.feature.user.categories.UserCategory;
import br.community.feature.user.categories.UserCategoryRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link UserCategoryRepository}; tabela {@code USER_CATEGORY}. */
@NullMarked
public final class UserCategoryJDBCRepository implements UserCategoryRepository {

    private static final String COLUMNS = "ID, COD_USER, TXT_NATURE, TXT_NAME, COD_PARENT, BOL_SYSTEM, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT";

    private final DataSource dataSource;

    public UserCategoryJDBCRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<UserCategory> findAllByUser(UUID userId) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM USER_CATEGORY WHERE COD_USER = ?",
                List.of(new JDBCPreparedParameter(1, userId.toString())),
                this::toCategories
        ).getOrThrow();
    }

    @Override
    public Optional<UserCategory> findById(UUID id) {
        return dataSource.executeQuery(
                "SELECT " + COLUMNS + " FROM USER_CATEGORY WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toCategories
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public UserCategory save(UserCategory entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        @Nullable String parentStr = entity.parentId() == null ? null : entity.parentId().toString();
        val systemFlag = entity.isSystem() ? "Y" : "N";
        val activeFlag = entity.active() ? "Y" : "N";
        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO USER_CATEGORY (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.userId().toString()),
                    new JDBCPreparedParameter(3, entity.nature().name()),
                    new JDBCPreparedParameter(4, entity.name()),
                    new JDBCPreparedParameter(5, parentStr),
                    new JDBCPreparedParameter(6, systemFlag),
                    new JDBCPreparedParameter(7, activeFlag),
                    new JDBCPreparedParameter(8, now),
                    new JDBCPreparedParameter(9, now)
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE USER_CATEGORY SET TXT_NATURE = ?, TXT_NAME = ?, COD_PARENT = ?, BOL_SYSTEM = ?, FLG_ACTIVE = ?, TMS_UPDATED_AT = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.nature().name()),
                    new JDBCPreparedParameter(2, entity.name()),
                    new JDBCPreparedParameter(3, parentStr),
                    new JDBCPreparedParameter(4, systemFlag),
                    new JDBCPreparedParameter(5, activeFlag),
                    new JDBCPreparedParameter(6, now),
                    new JDBCPreparedParameter(7, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM USER_CATEGORY WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    private List<UserCategory> toCategories(JDBCResultSet rs) {
        val categories = new ArrayList<UserCategory>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) categories.add(toCategory(rs));
        return categories;
    }

    private UserCategory toCategory(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").getOrThrow());
        val userId = UUID.fromString(rs.getString("COD_USER").getOrThrow());
        val nature = Transaction.Type.valueOf(rs.getString("TXT_NATURE").getOrThrow());
        val name = rs.getString("TXT_NAME").getOrThrow();
        final boolean isSystem = "Y".equals(rs.getString("BOL_SYSTEM").getOrThrow());
        final boolean active = "Y".equals(rs.getString("FLG_ACTIVE").getOrThrow());

        @Nullable String parentRaw = rs.getString("COD_PARENT").getOrThrow();
        @Nullable UUID parentId = (parentRaw == null || parentRaw.isBlank()) ? null : UUID.fromString(parentRaw);

        @Nullable Timestamp createRaw = rs.getTimestamp("TMS_CREATE_AT").getOrThrow();
        @Nullable LocalDateTime createdAt = createRaw == null ? null : createRaw.toLocalDateTime();
        @Nullable Timestamp updateRaw = rs.getTimestamp("TMS_UPDATED_AT").getOrThrow();
        @Nullable LocalDateTime updatedAt = updateRaw == null ? null : updateRaw.toLocalDateTime();

        return new UserCategory(id, userId, nature, name, parentId, isSystem, active, createdAt, updatedAt);
    }
}

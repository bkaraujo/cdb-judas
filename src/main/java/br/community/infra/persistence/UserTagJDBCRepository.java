package br.community.infra.persistence;

import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCPreparedParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.tags.UserTag;
import br.community.feature.user.tags.UserTagRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link UserTagRepository}; tabela {@code USER_TAG}. */
@NullMarked
public final class UserTagJDBCRepository implements UserTagRepository {

    private static final String COLUMNS = "ID, COD_USER, TXT_DESCRIPTION, TXT_COLOR, TMS_CREATE_AT";

    private final DataSource dataSource = Registry.get(DataSource.class);

    @Override
    public List<UserTag> findAllByUser(UUID userId) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_TAG WHERE COD_USER = ?",
                List.of(new JDBCPreparedParameter(1, userId.toString())),
                this::toTags
        ).getOrThrow();
    }

    @Override
    public Optional<UserTag> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_TAG WHERE ID = ?",
                List.of(new JDBCPreparedParameter(1, id.toString())),
                this::toTags
        ).getOrThrow().stream().findFirst();
    }

    @Override
    public UserTag save(UserTag entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val now = Timestamp.valueOf(LocalDateTime.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO USER_TAG (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
                    new JDBCPreparedParameter(1, entity.id().toString()),
                    new JDBCPreparedParameter(2, entity.userId().toString()),
                    new JDBCPreparedParameter(3, entity.name()),
                    new JDBCPreparedParameter(4, entity.color()),
                    new JDBCPreparedParameter(5, now)
            ).getOrThrow();
        } else {
            dataSource.execute(
                    "UPDATE USER_TAG SET TXT_DESCRIPTION = ?, TXT_COLOR = ? WHERE ID = ?",
                    new JDBCPreparedParameter(1, entity.name()),
                    new JDBCPreparedParameter(2, entity.color()),
                    new JDBCPreparedParameter(3, entity.id().toString())
            ).getOrThrow();
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM USER_TAG WHERE ID = ?",
                new JDBCPreparedParameter(1, id.toString())).getOrThrow();
    }

    private List<UserTag> toTags(JDBCResultSet rs) {
        val tags = new ArrayList<UserTag>();
        while (Boolean.TRUE.equals(rs.next().getOrThrow())) tags.add(toTag(rs));
        return tags;
    }

    private UserTag toTag(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").getOrThrow());
        val userId = UUID.fromString(rs.getString("COD_USER").getOrThrow());
        val name = rs.getString("TXT_DESCRIPTION").getOrThrow();
        val color = rs.getString("TXT_COLOR").getOrThrow();
        @Nullable Timestamp createRaw = rs.getTimestamp("TMS_CREATE_AT").getOrThrow();
        @Nullable LocalDateTime createdAt = createRaw == null ? null : createRaw.toLocalDateTime();
        return new UserTag(id, userId, name, color, createdAt);
    }
}

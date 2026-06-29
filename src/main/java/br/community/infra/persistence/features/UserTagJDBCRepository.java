package br.community.infra.persistence.features;

import br.commons.Registry;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.feature.user.tags.UserTag;
import br.community.feature.user.tags.UserTagRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.sql.Timestamp;
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
                JDBCParameter.of(userId.toString()),
                this::toTags
        );
    }

    @Override
    public Optional<UserTag> findById(UUID id) {
        return dataSource.query(
                "SELECT " + COLUMNS + " FROM USER_TAG WHERE ID = ?",
                JDBCParameter.of(id.toString()),
                this::toTags
        ).stream().findFirst();
    }

    @Override
    public UserTag save(UserTag entity) {
        val existing = findById(entity.id());
        if (existing.isPresent() && existing.get().equals(entity)) return entity;

        val now = Timestamp.valueOf(Time.now());

        if (existing.isEmpty()) {
            dataSource.execute(
                    "INSERT INTO USER_TAG (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
                    JDBCParameter.of (
                            entity.id().toString(),
                            entity.userId().toString(),
                            entity.name(),
                            entity.color(),
                            now
                    )
            );
        } else {
            dataSource.execute(
                    "UPDATE USER_TAG SET TXT_DESCRIPTION = ?, TXT_COLOR = ? WHERE ID = ?",
                    JDBCParameter.of (
                            entity.name(),
                            entity.color(),
                            entity.id().toString()
                    )
            );
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        dataSource.execute("DELETE FROM USER_TAG WHERE ID = ?",
                JDBCParameter.of(id.toString()));
    }

    private List<UserTag> toTags(JDBCResultSet rs) {
        val tags = new ArrayList<UserTag>();
        while (rs.next().get()) tags.add(toTag(rs));
        return tags;
    }

    private UserTag toTag(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val userId = UUID.fromString(rs.getString("COD_USER").get());
        val name = rs.getString("TXT_DESCRIPTION").get();
        val color = rs.getString("TXT_COLOR").get();
        val createRaw = rs.getTimestamp("TMS_CREATE_AT").get();
        val createdAt = createRaw.toLocalDateTime();
        return new UserTag(id, userId, name, color, createdAt);
    }
}

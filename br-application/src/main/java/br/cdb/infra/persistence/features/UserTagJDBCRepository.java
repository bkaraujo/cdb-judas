package br.cdb.infra.persistence.features;

import br.cdb.feature.finance.tags.UserTag;
import br.cdb.feature.finance.tags.UserTagRepository;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;

/** Adaptador JDBC (H2) da porta {@link UserTagRepository}; tabela {@code USER_TAG}. */
@NullMarked
public final class UserTagJDBCRepository extends JDBCRepository<UserTag> implements UserTagRepository {

    public UserTagJDBCRepository() {
        super("USER_TAG");
    }

    @Override
    public List<UserTag> findAllByUser(UUID userId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_USER = ?",
                JDBCParameter.of(userId.toString()),
                this::mapList
        );
    }

    @Override
    public Optional<UserTag> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    /** {@code USER_TAG} não tem TMS_UPDATED_AT; update só altera descrição/cor. */
    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("COD_USER", "TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(UserTag entity) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("COD_USER", entity.userId().toString());
        values.put("TXT_DESCRIPTION", entity.name());
        values.put("TXT_COLOR", entity.color());
        values.put("TMS_CREATE_AT", Timestamp.valueOf(Time.now()));
        return values;
    }

    @Override
    protected UserTag map(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val userId = UUID.fromString(rs.getString("COD_USER").get());
        val name = rs.getString("TXT_DESCRIPTION").get();
        val color = rs.getString("TXT_COLOR").get();
        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        return new UserTag(id, userId, name, color, createdAt);
    }
}

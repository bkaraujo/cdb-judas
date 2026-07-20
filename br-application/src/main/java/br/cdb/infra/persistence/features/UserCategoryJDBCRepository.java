package br.cdb.infra.persistence.features;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.finance.categories.UserCategory;
import br.cdb.feature.finance.categories.UserCategoryRepository;
import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;

/** Adaptador JDBC (H2) da porta {@link UserCategoryRepository}; tabela {@code PERSON_CATEGORY}. */
@NullMarked
public final class UserCategoryJDBCRepository extends JDBCRepository<UserCategory> implements UserCategoryRepository {

    public UserCategoryJDBCRepository() {
        super("PERSON_CATEGORY");
    }

    @Override
    public Optional<UserCategory> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public List<UserCategory> findAllByPerson(UUID personId) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_PERSON = ?",
                JDBCParameter.of(personId.toString()),
                this::mapList
        );
    }

    @Override
    public List<UserCategory> findByNature(UUID personId, Transaction.Type nature) {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE COD_PERSON = ? AND COD_NATURE = ?",
                JDBCParameter.of(personId.toString(), nature.name()),
                this::mapList
        );
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(UserCategory entity) {
        final @Nullable String parentStr = entity.parentId() == null ? null : entity.parentId().toString();
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("COD_PERSON", entity.personId().toString());
        values.put("COD_NATURE", entity.nature().name());
        values.put("TXT_NAME", entity.name());
        values.put("COD_PARENT", parentStr);
        values.put("FLG_SYSTEM", entity.isSystem() ? "Y" : "N");
        values.put("FLG_ACTIVE", entity.active() ? "Y" : "N");
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    @Override
    protected UserCategory map(JDBCResultSet rs) {
        val id = UUID.fromString(rs.getString("ID").get());
        val personId = UUID.fromString(rs.getString("COD_PERSON").get());
        val nature = Transaction.Type.valueOf(rs.getString("COD_NATURE").get());
        val name = rs.getString("TXT_NAME").get();
        val isSystem = "Y".equals(rs.getString("FLG_SYSTEM").get());
        val active = "Y".equals(rs.getString("FLG_ACTIVE").get());

        final @Nullable String parentRaw = rs.getString("COD_PARENT").get();
        final @Nullable UUID parentId = (parentRaw == null || parentRaw.isBlank()) ? null : UUID.fromString(parentRaw);

        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();

        return new UserCategory(id, personId, nature, name, parentId, isSystem, active, createdAt, updatedAt);
    }
}

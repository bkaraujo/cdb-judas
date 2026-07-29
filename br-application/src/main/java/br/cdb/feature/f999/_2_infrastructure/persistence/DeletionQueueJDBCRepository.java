package br.cdb.feature.f999._2_infrastructure.persistence;

import br.cdb.feature.f999._0_domain.DeletionQueueEntry;
import br.cdb.feature.f999._0_domain.DeletionQueueRepository;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;

/** Adaptador JDBC (H2) da porta {@link DeletionQueueRepository}; tabela {@code F999_DELETION_QUEUE}. */
@NullMarked
public final class DeletionQueueJDBCRepository extends JDBCRepository<DeletionQueueEntry> implements DeletionQueueRepository {

    public DeletionQueueJDBCRepository() {
        super("F999_DELETION_QUEUE");
    }

    @Override
    public Optional<DeletionQueueEntry> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    @Override
    public List<DeletionQueueEntry> findAllUnlocked() {
        return datasource.query(
                "SELECT " + columnList() + " FROM " + table() + " WHERE FLG_LOCKED = 'N' ORDER BY TMS_CREATE_AT",
                this::mapList
        );
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("TMS_CREATE_AT");
    }

    @Override
    protected Map<String, @Nullable Object> values(DeletionQueueEntry entity) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("TXT_TYPE", entity.type());
        values.put("COD_TARGET", entity.targetId().toString());
        values.put("COD_PERSON", entity.personId().toString());
        values.put("NUM_ATTEMPTS", entity.attempts());
        values.put("FLG_LOCKED", entity.locked() ? "Y" : "N");
        values.put("TMS_CREATE_AT", Timestamp.valueOf(entity.createdAt()));
        values.put("TMS_UPDATED_AT", Timestamp.valueOf(entity.updatedAt()));
        return values;
    }

    @Override
    protected DeletionQueueEntry map(JDBCResultSet rs) {
        return new DeletionQueueEntry(
                UUID.fromString(rs.getString("ID").get()),
                rs.getString("TXT_TYPE").get(),
                UUID.fromString(rs.getString("COD_TARGET").get()),
                UUID.fromString(rs.getString("COD_PERSON").get()),
                rs.getInt("NUM_ATTEMPTS").get(),
                "Y".equals(rs.getString("FLG_LOCKED").get()),
                rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime(),
                rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime()
        );
    }
}

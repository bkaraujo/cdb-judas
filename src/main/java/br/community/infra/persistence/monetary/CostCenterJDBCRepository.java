package br.community.infra.persistence.monetary;

import br.commons.chrono.Time;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Adaptador JDBC (H2) da porta {@link CostCenterRepository}; tabela {@code MON_COST_CENTER}. */
@NullMarked
public final class CostCenterJDBCRepository extends JDBCRepository<CostCenter> implements CostCenterRepository {

    public CostCenterJDBCRepository() {
        super("MON_COST_CENTER");
    }

    @Override
    public Optional<CostCenter> findById(UUID id) {
        return findById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        deleteById(id.toString());
    }

    @Override
    public void clearCache() {
        // Sem cache: a fonte de verdade é o próprio banco.
    }

    /** {@code FLG_ACTIVE} não está no domínio: semeado como 'Y' na inserção e preservado no update. */
    @Override
    protected Set<String> updateImmutableColumns() {
        return Set.of("TMS_CREATE_AT", "FLG_ACTIVE");
    }

    @Override
    protected Map<String, @Nullable Object> values(CostCenter entity) {
        val now = Timestamp.valueOf(Time.now());

        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("TXT_DESCRIPTION", entity.description());
        values.put("FLG_ACTIVE", "Y");
        values.put("TMS_CREATE_AT", now);
        values.put("TMS_UPDATED_AT", now);
        return values;
    }

    @Override
    protected CostCenter map(JDBCResultSet rs) {
        val createdAt = rs.getTimestamp("TMS_CREATE_AT").get().toLocalDateTime();
        val updatedAt = rs.getTimestamp("TMS_UPDATED_AT").get().toLocalDateTime();

        return new CostCenter(
                UUID.fromString(rs.getString("ID").get()),
                rs.getString("TXT_DESCRIPTION").get(),
                createdAt,
                updatedAt
        );
    }
}

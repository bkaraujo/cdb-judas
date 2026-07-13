package br.cdb.infra.persistence.monetary;

import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._0_domain.repository.CostCenterRepository;
import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

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
        return Set.of("FLG_ACTIVE");
    }

    @Override
    protected Map<String, @Nullable Object> values(CostCenter entity) {
        val values = new LinkedHashMap<String, @Nullable Object>();
        values.put("ID", entity.id().toString());
        values.put("TXT_DESCRIPTION", entity.description());
        values.put("FLG_ACTIVE", "Y");
        return values;
    }

    @Override
    protected CostCenter map(JDBCResultSet rs) {
        return new CostCenter(
                UUID.fromString(rs.getString("ID").get()),
                rs.getString("TXT_DESCRIPTION").get()
        );
    }
}

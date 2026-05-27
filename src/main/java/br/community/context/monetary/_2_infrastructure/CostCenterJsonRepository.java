package br.community.context.monetary._2_infrastructure;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.AbstractJsonRepository;
import br.community.context.monetary._0_domain.model.MonetaryCenter;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public final class CostCenterJsonRepository extends AbstractJsonRepository<MonetaryCenter, UUID> implements CostCenterRepository {

    public CostCenterJsonRepository(ObjectMapper mapper, Storage storage) {
        super(mapper, storage, MonetaryCenter.class);
    }

    @Override
    protected String jsonKey() { return "costCenters"; }

    @Override
    protected UUID getId(MonetaryCenter entity) { return entity.id(); }
}

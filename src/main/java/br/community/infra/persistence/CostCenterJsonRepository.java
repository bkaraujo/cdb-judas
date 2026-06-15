package br.community.infra.persistence;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.AbstractJsonRepository;
import br.community.context.monetary._0_domain.model.MonetaryCenter;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.ObjectMapper;

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

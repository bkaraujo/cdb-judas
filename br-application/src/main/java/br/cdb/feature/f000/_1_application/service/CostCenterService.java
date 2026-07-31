package br.cdb.feature.f000._1_application.service;

import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f000._0_domain.repository.CostCenterRepository;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public class CostCenterService {

    private final CostCenterRepository repository = Context.get(CostCenterRepository.class);

    public List<CostCenter> findAll() {
        return repository.findAll();
    }

    public CostCenter save(UUID id, String description) {
        return repository.save(new CostCenter(id, description));
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

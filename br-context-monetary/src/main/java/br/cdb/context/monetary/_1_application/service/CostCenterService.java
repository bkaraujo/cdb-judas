package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._0_domain.repository.CostCenterRepository;
import br.commons.Registry;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public class CostCenterService {

    private final CostCenterRepository repository = Registry.get(CostCenterRepository.class);

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

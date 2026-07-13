package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._0_domain.repository.CostCenterRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class CostCenterService {

    private final CostCenterRepository costCenterRepository;

    public List<CostCenter> findAll() {
        return costCenterRepository.findAll();
    }

    public CostCenter save(UUID id, String description) {
        return costCenterRepository.save(new CostCenter(id, description));
    }

    public void deleteById(UUID id) {
        costCenterRepository.deleteById(id);
    }
}

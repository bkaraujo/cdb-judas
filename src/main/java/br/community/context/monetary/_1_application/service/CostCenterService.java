package br.community.context.monetary._1_application.service;

import br.community.context.monetary._0_domain.model.MonetaryCenter;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class CostCenterService {

    private final CostCenterRepository costCenterRepository;

    public List<MonetaryCenter> findAll() {
        return costCenterRepository.findAll();
    }

    public MonetaryCenter save(UUID id, String description) {
        return costCenterRepository.save(new MonetaryCenter(id, description));
    }

    public void deleteById(UUID id) {
        costCenterRepository.deleteById(id);
    }
}

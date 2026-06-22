package br.community.context.monetary._1_application.usecase;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._1_application.command.CostCenterCommand;
import br.community.context.monetary._1_application.service.CostCenterService;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class MetadataUseCase {

    private final CostCenterService costCenterService;

    public Result<List<CostCenter>, DomainError> listCostCenters() {
        return Result.success(costCenterService.findAll());
    }

    public Result<CostCenter, DomainError> createCostCenter(CostCenterCommand cmd) {
        val created = costCenterService.save(UUID.randomUUID(), cmd.description());
        return Result.success(created);
    }

    public Result<CostCenter, DomainError> updateCostCenter(UUID id, CostCenterCommand cmd) {
        val updated = costCenterService.save(id, cmd.description());
        return Result.success(updated);
    }

    public Result<Void, DomainError> deleteCostCenter(UUID id) {
        costCenterService.deleteById(id);
        return Result.success();
    }
}

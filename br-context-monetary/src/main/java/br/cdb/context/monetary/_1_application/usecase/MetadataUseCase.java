package br.cdb.context.monetary._1_application.usecase;

import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._1_application.command.CostCenterCommand;
import br.cdb.context.monetary._1_application.service.CostCenterService;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class MetadataUseCase {

    private final CostCenterService costCenterService;

    public Result<List<CostCenter>, BusinessError> listCostCenters() {
        return Result.success(costCenterService.findAll());
    }

    public Result<CostCenter, BusinessError> createCostCenter(CostCenterCommand cmd) {
        val created = costCenterService.save(UUID.randomUUID(), cmd.description());
        return Result.success(created);
    }

    public Result<CostCenter, BusinessError> updateCostCenter(UUID id, CostCenterCommand cmd) {
        val updated = costCenterService.save(id, cmd.description());
        return Result.success(updated);
    }

    public Result<Void, BusinessError> deleteCostCenter(UUID id) {
        costCenterService.deleteById(id);
        return Result.success();
    }
}

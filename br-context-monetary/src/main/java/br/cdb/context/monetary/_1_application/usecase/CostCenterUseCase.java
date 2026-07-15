package br.cdb.context.monetary._1_application.usecase;

import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._1_application.command.CostCenterCommand;
import br.cdb.context.monetary._1_application.service.CostCenterService;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public class CostCenterUseCase {

    private final CostCenterService costCenterService = Registry.tryGet(CostCenterService.class);

    public Result<List<CostCenter>, BusinessError> listCostCenters() {
        return Result.success(costCenterService.findAll());
    }
    public Result<CostCenter, BusinessError> upsert(CostCenterCommand cmd) {
        return switch (cmd) {
            case CostCenterCommand.Create(var description) -> {
                val created = costCenterService.save(UUID.randomUUID(), description);
                yield Result.success(created);
            }
            case CostCenterCommand.Update(var id, var description) -> {
                val updated = costCenterService.save(id, description);
                yield Result.success(updated);
            }
            default -> Result.failure(new BusinessError.NotFound("Unkown command"));
        };
    }

    public Result<Void, BusinessError> delete(CostCenterCommand.Delete command) {
        costCenterService.deleteById(command.id());
        return Result.success();
    }
}

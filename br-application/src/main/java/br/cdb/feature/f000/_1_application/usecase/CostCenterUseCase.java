package br.cdb.feature.f000._1_application.usecase;

import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f000._1_application.command.CostCenterCommand;
import br.cdb.feature.f000._1_application.service.CostCenterService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public class CostCenterUseCase {

    private final CostCenterService service = Context.get(CostCenterService.class);

    public Result<List<CostCenter>, BusinessError> list() {
        return Result.success(service.findAll());
    }
    public Result<CostCenter, BusinessError> upsert(CostCenterCommand.Upsert cmd) {
        return switch (cmd) {
            case CostCenterCommand.Create(var description) -> {
                val created = service.save(UUID.randomUUID(), description);
                yield Result.success(created);
            }
            case CostCenterCommand.Update(var id, var description) -> {
                val updated = service.save(id, description);
                yield Result.success(updated);
            }
        };
    }

    public Result<Void, BusinessError> delete(CostCenterCommand.Delete command) {
        service.deleteById(command.id());
        return Result.success();
    }
}

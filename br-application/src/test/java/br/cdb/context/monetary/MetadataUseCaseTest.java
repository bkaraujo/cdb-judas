package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._1_application.command.CostCenterCommand;
import br.cdb.context.monetary._1_application.service.CostCenterService;
import br.cdb.context.monetary._1_application.usecase.MetadataUseCase;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cobre §9 (centro de custo). Categorias e tags migraram para a camada feature (USER_CATEGORY / USER_TAG). */

class MetadataUseCaseTest {

    private InMemoryRepositories.CostCenters costCenterRepo;
    private MetadataUseCase useCase;

    @BeforeEach
    void setUp() {
        costCenterRepo = new InMemoryRepositories.CostCenters();
        useCase = new MetadataUseCase(new CostCenterService(costCenterRepo));
    }

    // ── §9 CostCenter ─────────────────────────────────────────────

    @Test
    @DisplayName("§9.1 CRUD de centro de custo")
    void costCenterCrud() {
        Result<CostCenter, BusinessError> created = useCase.createCostCenter(new CostCenterCommand("Filial"));
        assertTrue(created.isSuccess());
        UUID id = ((Result.Success<CostCenter, BusinessError>) created).value().id();

        useCase.updateCostCenter(id, new CostCenterCommand("Matriz"));
        assertEquals("Matriz", costCenterRepo.findById(id).orElseThrow().description());

        useCase.deleteCostCenter(id);
        assertTrue(costCenterRepo.findById(id).isEmpty());
    }
}

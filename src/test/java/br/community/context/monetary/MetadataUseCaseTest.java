package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._1_application.command.CostCenterCommand;
import br.community.context.monetary._1_application.service.CostCenterService;
import br.community.context.monetary._1_application.usecase.MetadataUseCase;
import br.community.context.shared._0_domain.model.DomainError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
        Result<CostCenter, DomainError> created = useCase.createCostCenter(new CostCenterCommand("Filial"));
        assertTrue(created.isSuccess());
        UUID id = ((Result.Success<CostCenter, DomainError>) created).value().id();

        useCase.updateCostCenter(id, new CostCenterCommand("Matriz"));
        assertEquals("Matriz", costCenterRepo.findById(id).orElseThrow().description());

        useCase.deleteCostCenter(id);
        assertTrue(costCenterRepo.findById(id).isEmpty());
    }
}

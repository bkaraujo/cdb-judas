package br.cdb.context.monetary._1_application.usecase;

import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._0_domain.repository.CostCenterRepository;
import br.cdb.context.monetary._1_application.command.CostCenterCommand;
import br.cdb.context.monetary._1_application.service.CostCenterService;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cobre centro de custo. Categorias e tags são camada feature (USER_CATEGORY / USER_TAG), não este contexto. */
class MetadataUseCaseTest {

    private InMemoryRepositories.CostCenters costCenterRepo;
    private MetadataUseCase useCase;

    @BeforeEach
    void setUp() {
        Registry.remove(CostCenterService.class);

        costCenterRepo = new InMemoryRepositories.CostCenters();
        Registry.set(CostCenterRepository.class, costCenterRepo);

        useCase = new MetadataUseCase();
    }

    @Test
    @DisplayName("CRUD de centro de custo")
    void costCenterCrud() {
        Result<CostCenter, BusinessError> created = useCase.createCostCenter(new CostCenterCommand("Filial"));
        assertTrue(created.isSuccess());
        UUID id = ((Result.Success<CostCenter, BusinessError>) created).value().id();

        useCase.updateCostCenter(id, new CostCenterCommand("Matriz"));
        assertEquals("Matriz", costCenterRepo.findById(id).orElseThrow().description());

        useCase.deleteCostCenter(id);
        assertTrue(costCenterRepo.findById(id).isEmpty());
    }

    @Test
    @DisplayName("listCostCenters retorna todos os centros salvos")
    void listsAllCostCenters() {
        useCase.createCostCenter(new CostCenterCommand("Filial"));
        useCase.createCostCenter(new CostCenterCommand("Matriz"));

        Result<List<CostCenter>, BusinessError> r = useCase.listCostCenters();

        assertTrue(r.isSuccess());
        List<String> descriptions = ((Result.Success<List<CostCenter>, BusinessError>) r).value().stream()
                .map(CostCenter::description).toList();
        assertEquals(List.of("Filial", "Matriz"), descriptions);
    }
}

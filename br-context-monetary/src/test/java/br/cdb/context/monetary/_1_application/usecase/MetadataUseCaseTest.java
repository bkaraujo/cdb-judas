package br.cdb.context.monetary._1_application.usecase;

import br.cdb.context.monetary.AbstractUseCaseTest;
import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._1_application.command.CostCenterCommand;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cobre centro de custo. Categorias e tags são camada feature (USER_CATEGORY / USER_TAG), não este contexto. */
class MetadataUseCaseTest extends AbstractUseCaseTest {

    private MetadataUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MetadataUseCase();
    }

    @Test
    @DisplayName("CRUD de centro de custo")
    void costCenterCrud() {
        Result<CostCenter, BusinessError> created = useCase.createCostCenter(new CostCenterCommand("Filial"));
        assertTrue(created.isSuccess());
        val id = ((Result.Success<CostCenter, BusinessError>) created).value().id();

        useCase.updateCostCenter(id, new CostCenterCommand("Matriz"));
        assertEquals("Matriz", costCenterRepository().findById(id).orElseThrow().description());

        useCase.deleteCostCenter(id);
        assertTrue(costCenterRepository().findById(id).isEmpty());
    }

    @Test
    @DisplayName("listCostCenters retorna todos os centros salvos")
    void listsAllCostCenters() {
        useCase.createCostCenter(new CostCenterCommand("Filial"));
        useCase.createCostCenter(new CostCenterCommand("Matriz"));

        val r = useCase.listCostCenters();

        assertTrue(r.isSuccess());
        val descriptions = ((Result.Success<List<CostCenter>, BusinessError>) r).value().stream().map(CostCenter::description).toList();
        assertEquals(List.of("Filial", "Matriz"), descriptions);
    }
}

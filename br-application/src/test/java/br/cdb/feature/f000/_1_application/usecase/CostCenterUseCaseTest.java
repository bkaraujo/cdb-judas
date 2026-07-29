package br.cdb.feature.f000._1_application.usecase;

import br.cdb.context.monetary.AbstractUseCaseTest;
import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f000._1_application.command.CostCenterCommand;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cobre centro de custo. Categorias e tags são camada feature (PERSON_CATEGORY / PERSON_TAG), não este contexto. */
class CostCenterUseCaseTest extends AbstractUseCaseTest {

    private CostCenterUseCase useCase = Registry.tryGet(CostCenterUseCase.class);

    @BeforeEach
    void setUp() {
        useCase = new CostCenterUseCase();
    }

    @Test
    @DisplayName("CRUD de centro de custo")
    void costCenterCrud() {
        val created = useCase.upsert(new CostCenterCommand.Create("Filial"));
        assertTrue(created.isSuccess());
        val id = ((Result.Success<CostCenter, BusinessError>) created).value().id();

        useCase.upsert(new CostCenterCommand.Update(id, "Matriz"));
        assertEquals("Matriz", costCenterRepository().findById(id).orElseThrow().description());

        useCase.delete(new CostCenterCommand.Delete(id));
        assertTrue(costCenterRepository().findById(id).isEmpty());
    }

    @Test
    @DisplayName("list retorna todos os centros salvos")
    void listsAllCostCenters() {
        useCase.upsert(new CostCenterCommand.Create("Matriz"));
        useCase.upsert(new CostCenterCommand.Create("Filial"));

        val r = useCase.list();

        assertTrue(r.isSuccess());
        val descriptions = ((Result.Success<List<CostCenter>, BusinessError>) r).value().stream().map(CostCenter::description).toList();
        assertEquals(List.of("Matriz", "Filial"), descriptions);
    }
}

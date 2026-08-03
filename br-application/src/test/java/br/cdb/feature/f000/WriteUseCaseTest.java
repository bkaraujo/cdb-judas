package br.cdb.feature.f000;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._1_application.command.CostCenterCommand;
import br.cdb.feature.f000._1_application.usecase.ReadUseCase;
import br.cdb.feature.f000._1_application.usecase.WriteUseCase;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre a mutação da fatia-base {@code f000} — o par de {@code ReadUseCaseTest}; era
 * {@code CostCenterUseCaseTest}, que só via centro de custo. Pessoa e centro de custo dividem o par
 * porque {@code f000} é o kernel plano (ver javadoc de {@code WriteUseCase}). Categorias e tags são
 * de outras fatias ({@code f005}/{@code f004}), não deste kernel.
 */
class WriteUseCaseTest extends AbstractUseCaseTest {

    // Construído só no setUp (nunca no campo): o construtor resolve PersonService/CostCenterService,
    // e quem os registra sobre os fakes é o @BeforeEach da superclasse (em produção, F000Module).
    private WriteUseCase useCase;

    @BeforeEach
    void setUp() {
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        useCase = new WriteUseCase();
    }

    // ── Centro de custo ────────────────────────────────────────────

    @Test
    @DisplayName("CRUD de centro de custo")
    void costCenterCrud() {
        val created = useCase.upsertCostCenter(new CostCenterCommand.Create("Filial"));
        assertTrue(created.isSuccess());
        val id = ((Result.Success<CostCenter, BusinessError>) created).value().id();

        useCase.upsertCostCenter(new CostCenterCommand.Update(id, "Matriz"));
        assertEquals("Matriz", costCenterRepository().findById(id).orElseThrow().description());

        useCase.deleteCostCenter(new CostCenterCommand.Delete(id));
        assertTrue(costCenterRepository().findById(id).isEmpty());
    }

    // ── Pessoa ─────────────────────────────────────────────────────

    @Test
    @DisplayName("registerPerson cunha o id e persiste nome/locale/idioma")
    void registersPerson() {
        val r = useCase.registerPerson("Bruno", "pt-BR", "pt-BR");

        assertTrue(r.isSuccess());
        val person = ((Result.Success<Person, BusinessError>) r).value();
        assertEquals("Bruno", person.name());
        assertEquals("pt-BR", person.locale());
        assertEquals("pt-BR", person.language());
        assertEquals(List.of(person.id()), personRepository().findAll().stream().map(Person::id).toList());
    }

    @Test
    @DisplayName("renamePerson persiste o novo nome")
    void renamesPerson() {
        val person = ((Result.Success<Person, BusinessError>) useCase.registerPerson("Antigo", "pt-BR", "pt-BR")).value();

        val r = useCase.renamePerson(person, "Novo");

        assertTrue(r.isSuccess());
        assertEquals("Novo", ((Result.Success<Person, BusinessError>) r).value().name());
        assertEquals("Novo", personRepository().findById(person.id()).orElseThrow().name());
    }
}

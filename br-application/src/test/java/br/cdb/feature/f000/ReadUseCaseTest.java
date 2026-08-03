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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a leitura da fatia-base {@code f000} — o par de {@code WriteUseCaseTest}. Pessoa e centro de
 * custo dividem o par porque {@code f000} é o kernel plano (ver javadoc de {@code ReadUseCase}).
 */
class ReadUseCaseTest extends AbstractUseCaseTest {

    private ReadUseCase reads;
    private WriteUseCase writes;

    @BeforeEach
    void setUp() {
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        reads = new ReadUseCase();
        writes = new WriteUseCase();
    }

    private Person seedPerson(String name) {
        return ((Result.Success<Person, BusinessError>) writes.registerPerson(name, "pt-BR", "pt-BR")).value();
    }

    @Test
    @DisplayName("person(UUID) devolve a pessoa existente")
    void findsPersonById() {
        val person = seedPerson("Bruno");

        val r = reads.person(person.id());

        assertTrue(r.isSuccess());
        assertEquals("Bruno", ((Result.Success<Person, BusinessError>) r).value().name());
    }

    @Test
    @DisplayName("person(String) aceita o id em texto — é como f001 chega aqui")
    void findsPersonByStringId() {
        val person = seedPerson("Bruno");

        val r = reads.person(person.id().toString());

        assertTrue(r.isSuccess());
        assertEquals(person.id(), ((Result.Success<Person, BusinessError>) r).value().id());
    }

    @Test
    @DisplayName("person inexistente → NotFound")
    void findUnknownPerson() {
        val r = reads.person(UUID.randomUUID());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Person, BusinessError>) r).error());
    }

    @Test
    @DisplayName("costCenters devolve o catálogo persistido, na ordem de inserção")
    void listsCostCenters() {
        writes.upsertCostCenter(new CostCenterCommand.Create("Matriz"));
        writes.upsertCostCenter(new CostCenterCommand.Create("Filial"));

        val r = reads.costCenters();

        assertTrue(r.isSuccess());
        val descriptions = ((Result.Success<List<CostCenter>, BusinessError>) r).value().stream()
                .map(CostCenter::description).toList();
        assertEquals(List.of("Matriz", "Filial"), descriptions);
    }
}

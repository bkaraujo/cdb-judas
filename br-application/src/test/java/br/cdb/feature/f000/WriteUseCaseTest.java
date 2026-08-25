package br.cdb.feature.f000;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f000._0_domain.model.Person;
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
 * Cobre a mutação da fatia-base {@code f000} — o par de {@code ReadUseCaseTest}.
 * Pessoa é o único assunto aqui desde que cost center foi migrado para a flag planned.
 */
class WriteUseCaseTest extends AbstractUseCaseTest {

    // Construído só no setUp (nunca no campo): o construtor resolve PersonService,
    // e quem o registra sobre os fakes é o @BeforeEach da superclasse (em produção, F000Module).
    private WriteUseCase useCase;

    @BeforeEach
    void setUp() {
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        useCase = new WriteUseCase();
    }

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

package br.community.context.people;

import br.commons.annotation.Facade;
import br.community.context.people._0_domain.model.Person;
import br.community.context.people._1_application.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fachada do contexto people. Atende à regra "feature acessa contexto apenas via Facade".
 * Expõe o cadastro de pessoas e o vínculo pessoa↔conta (tabela auxiliar). Prior art:
 * {@code MonetaryContext}.
 */
@NullMarked
@RequiredArgsConstructor
public class PeopleContext implements Facade {

    private final PersonService personService;

    public Person registerPerson(Person person) {
        return personService.save(person);
    }

    public Optional<Person> findPerson(UUID id) {
        return personService.findById(id);
    }

    public void linkAccount(UUID personId, UUID accountId) {
        personService.linkAccount(personId, accountId);
    }

    public void unlinkAccount(UUID personId, UUID accountId) {
        personService.unlinkAccount(personId, accountId);
    }

    public List<UUID> accountIdsOf(UUID personId) {
        return personService.accountIdsOf(personId);
    }

    public boolean owns(UUID personId, UUID accountId) {
        return personService.owns(personId, accountId);
    }
}

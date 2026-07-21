package br.cdb.context.people;

import br.cdb.context.people._0_domain.model.Person;
import br.cdb.context.people._1_application.service.PersonService;
import br.commons.Result;
import br.commons.annotation.Facade;
import br.commons.business.BusinessError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.UUID;

/** Fachada do contexto people. Expõe o CRUD de pessoa. */
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

    public Optional<Person> findPerson(String id) {
        return personService.findById(id);
    }

    /** Atualiza o nome de exibição da pessoa. */
    public Result<Person, BusinessError> renamePerson(Person person, String newName) {
        return personService.rename(person, newName);
    }
}

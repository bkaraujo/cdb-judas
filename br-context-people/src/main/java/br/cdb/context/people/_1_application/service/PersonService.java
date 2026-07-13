package br.cdb.context.people._1_application.service;

import br.cdb.context.people._0_domain.model.Person;
import br.cdb.context.people._0_domain.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.UUID;

/** Lógica do contexto people: CRUD de {@link Person}. */
@NullMarked
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public Person save(Person person) {
        return personRepository.save(person);
    }

    public Optional<Person> findById(UUID id) {
        return personRepository.findById(id);
    }
}

package br.cdb.feature.f000._1_application.service;

import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._0_domain.repository.PersonRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Lógica do contexto people: CRUD de {@link Person}. */
@NullMarked
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;

    public Person save(Person person) {
        return repository.save(person);
    }

    public Optional<Person> findById(UUID id) {
        return repository.findById(id);
    }

    public Optional<Person> findById(String id) {
        return findById(UUID.fromString(id));
    }

    public Result<Person, BusinessError> rename(Person person, String newName) {
        if (newName.equals(person.name())) return Result.success(person);
        if (newName.isBlank()) return Result.failure(new BusinessError.Validation("person.newNameBlank"));

        val newPerson = new Person(
                person.id(),
                newName,
                person.locale(),
                person.language(),
                person.createdAt(),
                LocalDateTime.now()
        );

        val saved = save(newPerson);
        return Result.success(saved);
    }
}

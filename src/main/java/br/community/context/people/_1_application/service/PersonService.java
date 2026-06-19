package br.community.context.people._1_application.service;

import br.community.context.people._0_domain.model.Person;
import br.community.context.people._0_domain.repository.PersonAccountRepository;
import br.community.context.people._0_domain.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lógica do contexto people: CRUD de {@link Person} e o vínculo pessoa↔conta. */
@NullMarked
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonAccountRepository personAccountRepository;

    public Person save(Person person) {
        return personRepository.save(person);
    }

    public Optional<Person> findById(UUID id) {
        return personRepository.findById(id);
    }

    public void linkAccount(UUID personId, UUID accountId) {
        personAccountRepository.link(personId, accountId);
    }

    public void unlinkAccount(UUID personId, UUID accountId) {
        personAccountRepository.unlink(personId, accountId);
    }

    public List<UUID> accountIdsOf(UUID personId) {
        return personAccountRepository.accountIdsOf(personId);
    }

    public boolean owns(UUID personId, UUID accountId) {
        return personAccountRepository.owns(personId, accountId);
    }
}

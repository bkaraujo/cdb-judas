package br.cdb.context.people._1_application.usecase;

import br.cdb.context.people._0_domain.model.Person;
import br.cdb.context.people._0_domain.repository.PersonRepository;
import br.cdb.context.people._1_application.service.PersonService;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public class PersonUseCase {
    private final PersonService service =
            Registry.tryGet(PersonService.class, () -> new PersonService(Registry.get(PersonRepository.class)));

    public Result<Person, BusinessError> findById(String id) {
        return findById(UUID.fromString(id));
    }

    public Result<Person, BusinessError> findById(UUID id) {
        return service.findById(id)
                .<Result<Person, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Person not found: " + id)));
    }

    public Result<Person, BusinessError> rename(Person person, String newName) {
        return service.rename(person, newName);
    }
}

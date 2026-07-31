package br.cdb.feature.f000._1_application.usecase;

import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public class PersonUseCase {
    private final PersonService service = Context.get(PersonService.class);

    /** Cria uma nova pessoa (dono de recursos). O login apenas a referencia depois. */
    public Result<Person, BusinessError> register(String name, String locale, String language) {
        return Result.success(service.save(new Person(UUID.randomUUID(), name, locale, language)));
    }

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

package br.cdb.context.people;

import br.cdb.context.people._0_domain.repository.PersonRepository;
import br.cdb.context.people._1_application.service.PersonService;
import br.commons.Registry;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/** Composition root do contexto people: monta {@link PersonService} e publica {@link PeopleContext}. */
@NullMarked
public final class PeopleBootstrap {

    private PeopleBootstrap() {}

    public static void register() {
        val service = new PersonService(Registry.get(PersonRepository.class));
        Registry.set(PeopleContext.class, () -> new PeopleContext(service));
    }
}

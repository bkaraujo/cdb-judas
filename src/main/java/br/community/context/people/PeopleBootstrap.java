package br.community.context.people;

import br.commons.Registry;
import br.community.context.people._0_domain.repository.PersonRepository;
import br.community.context.people._1_application.service.PersonService;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/** Composition root do contexto people: monta {@link PersonService} e publica {@link PeopleContext}. */
@NullMarked
public final class PeopleBootstrap {

    private PeopleBootstrap() {}

    public static void register() {
        val service = new PersonService(Registry.get(PersonRepository.class));
        Registry.set(PeopleContext.class, new PeopleContext(service));
    }
}

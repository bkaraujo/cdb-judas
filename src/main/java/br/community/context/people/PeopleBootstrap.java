package br.community.context.people;

import br.commons.Registry;
import br.community.context.people._0_domain.repository.PersonAccountRepository;
import br.community.context.people._0_domain.repository.PersonRepository;
import br.community.context.people._1_application.service.PersonService;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Composition root do contexto people (modelo jimmy): monta o {@link PersonService} e publica a
 * {@link PeopleContext} no {@link Registry}. Pré-condição: {@link PersonRepository} e
 * {@link PersonAccountRepository} já registrados (feito por {@code core.ContextBridge}).
 */
@NullMarked
public final class PeopleBootstrap {

    private PeopleBootstrap() {}

    public static void register() {
        val service = new PersonService(
                Registry.get(PersonRepository.class),
                Registry.get(PersonAccountRepository.class)
        );
        Registry.set(PeopleContext.class, new PeopleContext(service));
    }
}

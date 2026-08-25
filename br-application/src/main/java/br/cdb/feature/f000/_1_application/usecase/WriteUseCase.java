package br.cdb.feature.f000._1_application.usecase;

import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Toda a mutação da fatia-base {@code f000} — o par de {@link ReadUseCase}, mesmo arranjo CQRS de
 * {@code f001}–{@code f006}. Context-wired ({@code Context.tryGet(WriteUseCase.class)}, nunca
 * {@code @Inject}).
 *
 * <p>Ao contrário das outras fatias, o par de {@code f000} não serve a um {@code *Resource}: os
 * recursos daqui são o login (que acessa {@code UserRepository} direto, exceção nomeada no
 * {@code ArchitectureTest}), o SSE e a versão. Quem consome este par é <b>outra fatia</b> —
 * {@code f001.ProfileService} (renomear a pessoa) e o {@code UserService} da própria f000 (criar a
 * pessoa dona dos recursos, antes do login).
 */
@NullMarked
public class WriteUseCase {

    // ── Pessoa ─────────────────────────────────────────────────────

    private final PersonService service = Context.get(PersonService.class);

    /** Cria uma nova pessoa (dona dos recursos). O login apenas a referencia depois. */
    public Result<Person, BusinessError> registerPerson(String name, String locale, String language) {
        return Result.success(service.save(new Person(UUID.randomUUID(), name, locale, language)));
    }

    public Result<Person, BusinessError> renamePerson(Person person, String newName) {
        return service.rename(person, newName);
    }

}

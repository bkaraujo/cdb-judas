package br.cdb.feature.f000._1_application.usecase;

import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Toda a leitura da fatia-base {@code f000} — o par de {@link WriteUseCase}, mesmo arranjo CQRS de
 * {@code f001}–{@code f006}. Context-wired ({@code Context.tryGet(ReadUseCase.class)}, nunca
 * {@code @Inject}).
 *
 * <p>Como o {@link WriteUseCase}, não serve a um {@code *Resource} desta fatia: quem lê é
 * {@code f001.ProfileService} (a pessoa por trás de {@code /api/me}) e o {@code UserService} local.
 *
 * <p>Sem guarda de propriedade: a pessoa <b>é</b> a identidade autenticada (não há {@code {uuid}}
 * de terceiro a comparar).
 */
@NullMarked
public class ReadUseCase {

    // ── Pessoa ─────────────────────────────────────────────────────

    private final PersonService service = Context.get(PersonService.class);

    public Result<Person, BusinessError> person(String id) {
        return person(UUID.fromString(id));
    }

    public Result<Person, BusinessError> person(UUID id) {
        return service.findById(id)
                .<Result<Person, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("f000.person.notFound", id)));
    }

}

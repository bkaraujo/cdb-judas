package br.cdb.feature.f001._1_application.usecase;

import br.cdb.feature.f001._0_domain.model.Profile;
import br.cdb.feature.f001._1_application.service.ProfileService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

/**
 * Toda a mutação do próprio perfil da fatia {@code f001} — o par de {@link ReadUseCase}, mesmo
 * arranjo CQRS de {@code f002}–{@code f006}. Context-wired
 * ({@code Context.tryGet(WriteUseCase.class)}, nunca {@code @Inject}); o {@code SelfResource}
 * escreve <b>só</b> por aqui.
 *
 * <p>Sem guarda de propriedade nem evento: a rota é {@code /api/me} (o {@code personId} vem do token
 * autenticado) e o perfil não tem consumidor cross-slice — as preferências são write-through, lidas
 * de volta na mesma resposta.
 *
 * <p>Nota: o nome simples coincide com o {@code WriteUseCase} das demais fatias — quem precisa de
 * mais de um referencia os outros pelo nome completo.
 */
@NullMarked
public class WriteUseCase {

    private final ProfileService service = Context.tryGet(ProfileService.class);

    /** Atualiza o nome de exibição. Aplica trim; nome em branco é rejeitado. */
    public Result<Profile, BusinessError> updateName(String personId, String name) {
        return service.updateName(personId, name);
    }

    /** Aplica um patch parcial sobre as preferências (campo nulo mantém o valor atual). */
    public Result<Profile, BusinessError> updatePreferences(String personId, PreferencesPatch patch) {
        return service.updatePreferences(personId, patch);
    }
}

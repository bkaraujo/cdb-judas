package br.cdb.feature.f001._1_application.usecase;

import br.cdb.feature.f001._0_domain.Profile;
import br.cdb.feature.f001._1_application.service.ProfileService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

/**
 * Toda a leitura do próprio perfil da fatia {@code f001} — o par de {@link WriteUseCase}, mesmo
 * arranjo CQRS de {@code f002}–{@code f006}. Context-wired
 * ({@code Context.tryGet(ReadUseCase.class)}, nunca {@code @Inject}); o {@code SelfResource} lê
 * <b>só</b> daqui.
 *
 * <p>Sem guarda de propriedade: a rota é {@code /api/me}, sem {@code {uuid}} — o {@code personId}
 * vem do token autenticado, resolvido na borda e repassado. Sem identidade não se chega aqui (401
 * na cadeia de filtros).
 *
 * <p>Nota: o nome simples coincide com o {@code ReadUseCase} das demais fatias — quem precisa de
 * mais de um referencia os outros pelo nome completo.
 */
@NullMarked
public class ReadUseCase {

    private final ProfileService service = Context.tryGet(ProfileService.class);

    /** Nome/identidade ({@code Person}) + preferências; preferências ausentes viram os padrões. */
    public Result<Profile, BusinessError> profile(String personId) {
        return service.getProfile(personId);
    }
}

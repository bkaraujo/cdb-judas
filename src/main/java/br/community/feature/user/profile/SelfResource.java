package br.community.feature.user.profile;

import br.commons.Result;
import br.community.context.security.SecurityContext;
import br.community.context.shared._1_application.DomainException;
import br.community.core.web.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recurso {@code self}: a identidade vem do contexto autenticado (sem id no caminho →
 * sem risco de IDOR). Sem token, a cadeia de filtros responde 401 antes de chegar aqui.
 */
@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
public class SelfResource {

    private final SecurityContext securityContext;

    @GetMapping
    public MeResponse getMe() {
        return switch (securityContext.getMe(CurrentUser.getId())) {
            case Result.Success(var user) -> MeResponse.from(user);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }
}

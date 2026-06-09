package br.community.feature.user.profile;

import br.commons.Result;
import br.community.context.security.SecurityContext;
import br.community.context.security._0_domain.model.PreferencesPatch;
import br.community.context.shared._1_application.DomainException;
import br.community.core.web.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** PATCH parcial: aplica nome e/ou preferências de forma independente (merge). */
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public MeResponse update(@RequestBody @Valid UpdateMeRequest req) {
        val id = CurrentUser.getId();

        var result = req.name() != null
                ? securityContext.updateName(id, req.name())
                : securityContext.getMe(id);

        val prefs = req.preferences();
        if (prefs != null) {
            val patch = new PreferencesPatch(prefs.theme(), prefs.language(), prefs.locale(), prefs.sidebarCollapsed());
            result = result.flatMap(ignored -> securityContext.updatePreferences(id, patch));
        }

        return switch (result) {
            case Result.Success(var user) -> MeResponse.from(user);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }
}

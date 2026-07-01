package br.community.feature.user.profile;

import br.commons.Result;
import br.community.context.shared._1_application.DomainException;
import br.community.core.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Recurso {@code self}: a identidade vem do contexto autenticado (sem id no caminho →
 * sem risco de IDOR). Sem token, a cadeia de filtros responde 401 antes de chegar aqui.
 */
@NullMarked
@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class SelfResource {

    private final UserService userService;

    @GET
    public MeResponse getMe() {
        return switch (userService.getProfile(CurrentUser.getId())) {
            case Result.Success(var profile) -> MeResponse.from(profile);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    /** PATCH parcial: aplica nome e/ou preferências de forma independente (merge). */
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public MeResponse update(@Valid UpdateMeRequest req) {
        val id = CurrentUser.getId();

        var result = req.name() != null
                ? userService.updateName(id, req.name())
                : userService.getProfile(id);

        val prefs = req.preferences();
        if (prefs != null) {
            val patch = new PreferencesPatch(prefs.theme(), prefs.language(), prefs.locale(), prefs.sidebarCollapsed());
            result = result.flatMap(ignored -> userService.updatePreferences(id, patch));
        }

        return switch (result) {
            case Result.Success(var profile) -> MeResponse.from(profile);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }
}

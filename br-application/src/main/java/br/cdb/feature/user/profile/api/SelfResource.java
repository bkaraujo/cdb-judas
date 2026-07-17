package br.cdb.feature.user.profile.api;

import br.cdb.feature.user.UserUseCase;
import br.commons.Result;
import br.commons.business.BusinessException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Recurso {@code self}: a identidade vem do contexto autenticado (sem id no caminho →
 * sem risco de IDOR). Sem token, a cadeia de filtros responde 401 antes de chegar aqui.
 */
@NullMarked
@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class SelfResource {

    private final UserUseCase userUseCase;

    @GET
    public SelfResponse getMe() {
        return switch (userUseCase.profile()) {
            case Result.Success(var profile) -> SelfResponse.from(profile);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    /** PATCH parcial: aplica nome e/ou preferências de forma independente (merge). */
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public SelfResponse update(@Valid UpdateMeRequest req) {
        return switch (userUseCase.updateProfile(req.name(), toPatch(req))) {
            case Result.Success(var profile) -> SelfResponse.from(profile);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @Nullable
    private static PreferencesPatch toPatch(UpdateMeRequest req) {
        val prefs = req.preferences();
        if (prefs == null) return null;
        return new PreferencesPatch(prefs.theme(), prefs.language(), prefs.locale(), prefs.sidebarCollapsed());
    }
}

package br.cdb.feature.f001._2_infrastructure.web;

import br.cdb.core.persistence.repository.UserRepository;
import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f001._0_domain.model.Profile;
import br.cdb.feature.f001._1_application.usecase.PreferencesPatch;
import br.cdb.feature.f001._1_application.usecase.ReadUseCase;
import br.cdb.feature.f001._1_application.usecase.WriteUseCase;
import br.cdb.feature.f001._2_infrastructure.web.request.UpdateMeRequest;
import br.cdb.feature.f001._2_infrastructure.web.response.SelfResponse;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import br.commons.framework.cdi.Context;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Recurso {@code self}: a identidade vem do contexto autenticado (sem personId no caminho →
 * sem risco de IDOR). Sem token, a cadeia de filtros responde 401 antes de chegar aqui.
 * Orquestra o par {@link ReadUseCase}/{@link WriteUseCase} (Person + Preferences, fatia {@code f001}) e
 * {@link UserRepository#findByPersonId} (username de login) — a composição dos dois em
 * {@link SelfView} é específica deste recurso, não mora em nenhum dos dois.
 */
@NullMarked
@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class SelfResource {

    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);
    private final WriteUseCase writes = Context.tryGet(WriteUseCase.class);
    private final UserRepository userRepository = Context.get(UserRepository.class);

    @GET
    public SelfResponse getMe() {
        val personId = HTTPRequest.personId();
        return switch (self(personId, reads.profile(personId))) {
            case Result.Success(var self) -> SelfResponse.from(self);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    /** PATCH parcial: aplica nome e/ou preferências de forma independente (merge). */
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public SelfResponse update(@Valid UpdateMeRequest req) {
        val personId = HTTPRequest.personId();

        var result = req.name() != null
                ? writes.updateName(personId, req.name())
                : reads.profile(personId);

        val patch = toPatch(req);
        if (patch != null) {
            result = result.flatMap(ignored -> writes.updatePreferences(personId, patch));
        }

        return switch (self(personId, result)) {
            case Result.Success(var self) -> SelfResponse.from(self);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    private Result<SelfView, BusinessError> self(String personId, Result<Profile, BusinessError> profile) {
        return profile.flatMap(p -> username(personId).map(username -> new SelfView(p, username)));
    }

    private Result<String, BusinessError> username(String personId) {
        return userRepository.findByPersonId(personId)
                .<Result<String, BusinessError>>map(user -> Result.success(user.username()))
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("f001.user.notFound")));
    }

    @Nullable
    private static PreferencesPatch toPatch(UpdateMeRequest req) {
        val prefs = req.preferences();
        if (prefs == null) return null;
        return new PreferencesPatch(prefs.theme(), prefs.language(), prefs.locale(), prefs.sidebarCollapsed());
    }
}

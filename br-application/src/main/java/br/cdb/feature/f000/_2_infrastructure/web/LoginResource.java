package br.cdb.feature.f000._2_infrastructure.web;

import br.cdb.core.persistence.UserRepository;
import br.cdb.core.security.AccessTokenStore;
import br.cdb.feature.f000._2_infrastructure.web.request.LoginRequest;
import br.commons.Logger;
import br.commons.framework.cdi.Context;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import static br.cdb.core.web.HTTPRequest.TOKEN_HEADER;

@Path("/login")
@NullMarked
public class LoginResource {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository = Context.get(UserRepository.class);

    @Inject
    AccessTokenStore tokenStore;

    @POST
    public Response login(LoginRequest request) {
        val user = userRepository.findByUsername(request.username()).orElse(null);
        if (user == null) {
            Logger.debug("LOGIN => user '%s' not found", request.username());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (!BcryptUtil.matches(request.password(), user.password())) {
            Logger.debug("LOGIN => invalid password for '%s'", request.username());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        val personId = user.personId();
        if (personId == null) {
            Logger.debug("LOGIN => user '%s' has no linked person", request.username());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Token continua atado ao userId (login/credencial); a rota do cliente usa o personId.
        val token = tokenStore.persistent(user.id());
        Logger.debug("LOGIN => '%s' (person %s) issued token", request.username(), personId);
        return Response.ok()
                .header(TOKEN_HEADER, token)
                .header(USER_ID_HEADER, personId)
                .build();
    }
}

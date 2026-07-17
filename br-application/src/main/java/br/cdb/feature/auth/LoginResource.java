package br.cdb.feature.auth;

import br.cdb.core.web.security.UserRepository;
import br.cdb.core.web.security.core.AccessTokenStore;
import br.commons.Logger;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@Path("/login")
@NullMarked
public class LoginResource {

    public static final String TOKEN_HEADER = "X-Access-Token";
    public static final String USER_ID_HEADER = "X-User-Id";

    @Inject
    UserRepository userRepository;

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

        val token = tokenStore.issue(user.id());
        Logger.debug("LOGIN => '%s' (%s) issued token", request.username(), user.id());
        return Response.ok()
                .header(TOKEN_HEADER, token)
                .header(USER_ID_HEADER, user.id())
                .build();
    }
}

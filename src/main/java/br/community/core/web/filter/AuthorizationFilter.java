package br.community.core.web.filter;

import br.commons.Logger;
import br.community.core.web.RequestUtils;
import br.community.core.web.security.CurrentUserContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jspecify.annotations.NullMarked;

/** Exige usuário autenticado para qualquer rota não-estática (exceto OPTIONS). */
@Provider
@Priority(Priorities.AUTHORIZATION)
@NullMarked
public class AuthorizationFilter implements ContainerRequestFilter {

    @Inject
    CurrentUserContext currentUser;

    @Override
    public void filter(ContainerRequestContext request) {
        if (RequestUtils.isStatic(request) || HttpMethod.OPTIONS.equals(request.getMethod())) return;

        if (currentUser.user() == null) {
            Logger.debug("AUTHZ %s %s => denied (not authenticated)", request.getMethod(), RequestUtils.path(request));
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}

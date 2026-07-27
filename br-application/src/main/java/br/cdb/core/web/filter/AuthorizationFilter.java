package br.cdb.core.web.filter;

import br.cdb.core.web.HTTPRequest;
import br.commons.Logger;
import jakarta.annotation.Priority;
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


    @Override
    public void filter(ContainerRequestContext request) {
        if (HTTPRequest.isStatic(request) || HttpMethod.OPTIONS.equals(request.getMethod())) return;

        if (HTTPRequest.user() == null) {
            Logger.debug("AUTHZ %s %s => denied (not authenticated)", request.getMethod(), HTTPRequest.path(request));
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}

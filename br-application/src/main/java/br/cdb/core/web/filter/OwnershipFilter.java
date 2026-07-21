package br.cdb.core.web.filter;

import br.cdb.core.web.Request;
import br.cdb.core.web.security.AuthenticatedUser;
import br.commons.Logger;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Guarda de propriedade: o primeiro segmento após {@code /api/} é o dono da rota e deve coincidir
 * com o usuário autenticado. Rotas globais ({@code /api/cost-center*}, {@code /api/version}) e o
 * recurso self ({@code /api/me}) são isentos. Roda depois da autorização.
 */
@Provider
@Priority(Priorities.AUTHORIZATION + 10)
@NullMarked
public class OwnershipFilter implements ContainerRequestFilter {

    private static final String API_PREFIX = "/api/";

    @Override
    public void filter(ContainerRequestContext request) {
        val path = Request.path(request);
        if (!path.startsWith(API_PREFIX) || isExcluded(path)) return;

        val user = Request.get(Request.X_REQUEST_USER, AuthenticatedUser.class);
        if (user == null) {
            Logger.debug("OWNERSHIP %s %s => 401 (não autenticado)", request.getMethod(), path);
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        val routeUserId = firstSegment(path);
        if (!user.personId().equals(routeUserId)) {
            Logger.debug("OWNERSHIP %s %s => 403 (rota '%s' != usuário '%s')",
                    request.getMethod(), path, routeUserId, user.personId());
            request.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

    private boolean isExcluded(String path) {
        return path.equals("/api/cost-center") || path.startsWith("/api/cost-center/")
                || path.equals("/api/version")
                || path.equals("/api/me");
    }

    private String firstSegment(String path) {
        val rest = path.substring(API_PREFIX.length());
        val slash = rest.indexOf('/');
        return slash >= 0 ? rest.substring(0, slash) : rest;
    }
}

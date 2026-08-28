package br.cdb.core.web.filter;

import br.cdb.core.web.HTTPRequest;
import br.commons.framework.logger.MDC;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Correlação de requisição: injeta {@code X-REQUEST-ID} no MDC no início e, no fim, descarta o que a
 * requisição guardou ({@link HTTPRequest#clear}) e limpa as chaves do MDC (evita vazamento entre
 * threads de worker reaproveitadas).
 */
@Provider
@Priority(500)
@NullMarked
public class MDCLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request) {
        // isStatic() é o gate de autorização (isApi negado) — /login fica de fora dele de propósito
        // (rota pública, fora de /api/*), mas ainda é um endpoint real e merece correlação de log.
        if (HTTPRequest.isStatic(request) && !isLogin(request)) return;

        var xRequestId = request.getHeaderString(HTTPRequest.X_REQUEST_ID);
        if (xRequestId == null) { xRequestId = UUID.randomUUID().toString(); }

        MDC.push(HTTPRequest.X_REQUEST_ID, xRequestId);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        // Antes dos pop: clear() encontra o mapa da requisição pelo X-REQUEST-ID que ainda está no MDC.
        HTTPRequest.clear();
        MDC.pop(HTTPRequest.X_REQUEST_USER);
        MDC.pop(HTTPRequest.X_REQUEST_ID);
    }

    private static boolean isLogin(ContainerRequestContext request) {
        return "/login".equals(HTTPRequest.path(request));
    }
}

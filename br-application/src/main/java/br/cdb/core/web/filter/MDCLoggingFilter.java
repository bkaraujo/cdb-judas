package br.cdb.core.web.filter;

import br.cdb.core.web.RequestUtils;
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
 * Correlação de requisição: injeta {@code X-REQUEST-ID} no MDC no início e limpa as chaves
 * do MDC no fim (evita vazamento entre threads de worker reaproveitadas).
 */
@Provider
@Priority(500)
@NullMarked
public class MDCLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String REQUEST_ID = "X-REQUEST-ID";
    private static final String REQUEST_USER = "X-REQUEST-USER";

    @Override
    public void filter(ContainerRequestContext request) {
        if (RequestUtils.isStatic(request)) return;

        var xRequestId = request.getHeaderString(REQUEST_ID);
        if (xRequestId == null) { xRequestId = UUID.randomUUID().toString(); }

        MDC.push(REQUEST_ID, xRequestId);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        MDC.pop(REQUEST_USER);
        MDC.pop(REQUEST_ID);
    }
}

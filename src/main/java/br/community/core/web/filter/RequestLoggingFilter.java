package br.community.core.web.filter;

import br.commons.Logger;
import br.community.core.web.RequestUtils;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/** Log de requisição em nível TRACE (método + caminho). */
@Provider
@Priority(600)
@NullMarked
public class RequestLoggingFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext request) {
        val path = RequestUtils.path(request);
        if (!path.contains("favicon")) {
            Logger.trace("%s %s", request.getMethod(), path);
        }
    }
}

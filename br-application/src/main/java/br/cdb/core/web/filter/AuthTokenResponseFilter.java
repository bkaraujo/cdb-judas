package br.cdb.core.web.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import static br.cdb.core.web.HTTPRequest.TOKEN_HEADER;
import static br.cdb.core.web.filter.AuthenticationFilter.NEXT_TOKEN_PROPERTY;

/** Emite o header do token rotacionado, guardado pelo {@link AuthenticationFilter} no request. */
@Provider
@NullMarked
public class AuthTokenResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        val next = request.getProperty(NEXT_TOKEN_PROPERTY);
        if (next instanceof String token) {
            response.getHeaders().add(TOKEN_HEADER, token);
        }
    }
}

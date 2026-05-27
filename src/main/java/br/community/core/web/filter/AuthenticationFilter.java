package br.community.core.web.filter;

import br.commons.Logger;
import br.commons.framework.logger.MDC;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static br.community.core.web.security.LoginResource.TOKEN_HEADER;

@NullMarked
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final br.community.core.web.security.AccessTokenStore tokenStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        val token = request.getHeader(TOKEN_HEADER);
        if (token != null) {
            val isSse = "text/event-stream".equals(request.getHeader("Accept"));
            if (isSse) {
                tokenStore.validate(token).ifPresent(username -> {
                    val auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    Logger.debug("AUTHN %s %s => user '%s' authenticated (SSE)", request.getMethod(), request.getRequestURI(), username);
                    MDC.push("X-REQUEST-USER", username);
                });
            } else {
                val result = tokenStore.rotate(token);
                if (result.isPresent()) {
                    val rotation = result.get();
                    val auth = new UsernamePasswordAuthenticationToken(rotation.username(), null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    response.setHeader(TOKEN_HEADER, rotation.nextToken());
                    Logger.debug("AUTHN %s %s => user '%s' authenticated", request.getMethod(), request.getRequestURI(), rotation.username());
                    MDC.push("X-REQUEST-USER", rotation.username());
                } else {
                    Logger.debug("AUTHN %s %s => invalid or expired token", request.getMethod(), request.getRequestURI());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

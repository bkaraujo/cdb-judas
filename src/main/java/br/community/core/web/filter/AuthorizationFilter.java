package br.community.core.web.filter;

import br.commons.Logger;
import br.community.core.web.RequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@NullMarked
public class AuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (RequestUtils.isStatic(request) || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        val auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            Logger.debug("AUTHZ %s %s => denied (not authenticated)", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Logger.trace("AUTHZ %s %s => granted for '%s'", request.getMethod(), request.getRequestURI(), auth.getName());
        filterChain.doFilter(request, response);
    }
}

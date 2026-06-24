package br.community.core.web.security;

import br.commons.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guarda de propriedade: o primeiro segmento após {@code /api/} é o identificador do dono da rota
 * e deve coincidir com o usuário autenticado. Rotas globais e rotas legadas (ainda fora do namespace)
 * são isentas via {@code excludePathPatterns} em {@code WebConfig}.
 */
@Component
@NullMarked
public class OwnershipInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        val auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            Logger.debug("OWNERSHIP %s %s => 401 (não autenticado)", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        val routeUserId = firstSegment(request.getRequestURI());
        if (!user.id().equals(routeUserId)) {
            Logger.debug("OWNERSHIP %s %s => 403 (rota '%s' != usuário '%s')",
                    request.getMethod(), request.getRequestURI(), routeUserId, user.id());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private String firstSegment(String uri) {
        val idx = uri.indexOf("/api/");
        if (idx < 0) return "";
        val rest = uri.substring(idx + 5);
        val slash = rest.indexOf('/');
        return slash >= 0 ? rest.substring(0, slash) : rest;
    }
}

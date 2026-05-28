package br.community.core.web.security;

import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@NullMarked
public abstract class CurrentUser {
    private CurrentUser() {}

    public static String getId() {
        val principal = principal();
        if (principal instanceof AuthenticatedUser user) {
            return user.id();
        }
        throw new IllegalStateException("Usuário autenticado sem identificador no contexto");
    }

    public static String getUsername() {
        val principal = principal();
        if (principal instanceof AuthenticatedUser user) {
            return user.username();
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    private static Object principal() {
        val auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto");
        }
        val principal = auth.getPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto");
        }
        return principal;
    }
}

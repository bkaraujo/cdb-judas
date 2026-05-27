package br.community.core.web.security;

import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@NullMarked
public abstract class CurrentUser {
    private CurrentUser() {}

    public static String getUsername() {
        val auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto");
        }

        val principal = auth.getPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto");
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        return principal.toString();
    }
}

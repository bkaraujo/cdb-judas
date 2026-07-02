package br.community.core.web.security;

import io.quarkus.arc.Arc;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Fachada estática para a identidade autenticada, resolvendo o {@link CurrentUserContext}
 * {@code @RequestScoped} via {@code Arc}. Mantém os call-sites inalterados após a migração
 * do {@code SecurityContextHolder}.
 */
@NullMarked
public abstract class CurrentUser {
    private CurrentUser() {}

    public static String getId() {
        return current().id();
    }

    public static String getUsername() {
        return current().username();
    }

    private static AuthenticatedUser current() {
        val handle = Arc.container().instance(CurrentUserContext.class);
        val ctx = handle.get();
        val user = ctx == null ? null : ctx.user();
        if (user == null) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto");
        }
        return user;
    }
}

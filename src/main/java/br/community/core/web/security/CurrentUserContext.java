package br.community.core.web.security;

import jakarta.enterprise.context.RequestScoped;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Portador da identidade autenticada no escopo da requisição — substitui o
 * {@code SecurityContextHolder} do Spring. Populado pelo {@code AuthenticationFilter}
 * e lido via {@link CurrentUser}.
 */
@RequestScoped
@NullMarked
public class CurrentUserContext {

    private @Nullable AuthenticatedUser user;

    public void set(AuthenticatedUser user) {
        this.user = user;
    }

    public @Nullable AuthenticatedUser user() {
        return user;
    }
}

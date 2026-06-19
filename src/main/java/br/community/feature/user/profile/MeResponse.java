package br.community.feature.user.profile;

import br.community.core.web.security.Preferences;
import br.community.core.web.security.User;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Projeção de leitura/escrita do recurso {@code self} (/api/me). Não expõe a senha do
 * agregado. {@code preferences} reutiliza o record de domínio (formato 1:1 com o contrato).
 */
@NullMarked
public record MeResponse(
        String id,
        String username,
        @Nullable String name,
        Preferences preferences
) {
    public static MeResponse from(User user) {
        return new MeResponse(user.id(), user.username(), user.name(), user.preferences());
    }
}

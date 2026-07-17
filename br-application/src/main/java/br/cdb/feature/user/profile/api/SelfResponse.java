package br.cdb.feature.user.profile.api;

import br.cdb.feature.user.profile.preference.Preferences;
import br.cdb.feature.user.profile.Profile;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Projeção de leitura/escrita do recurso {@code self} (/api/me). Não expõe a senha do
 * agregado. {@code preferences} reutiliza o record de domínio (formato 1:1 com o contrato).
 */
@NullMarked
public record SelfResponse(
        String id,
        String username,
        @Nullable String name,
        Preferences preferences
) {
    public static SelfResponse from(Profile profile) {
        val user = profile.user();
        return new SelfResponse(user.id(), user.username(), user.name(), profile.preferences());
    }
}

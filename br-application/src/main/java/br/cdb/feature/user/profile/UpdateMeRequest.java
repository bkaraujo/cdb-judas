package br.cdb.feature.user.profile;

import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Corpo parcial do PATCH /api/me — DTO de entrada distinto do record de domínio (todos
 * os campos anuláveis). Campo ausente/nulo não é alterado; {@code name} em branco é
 * permitido (a exibição cai para o username).
 */
@NullMarked
public record UpdateMeRequest(
        @Size(max = 80) @Nullable String name,
        @Nullable PreferencesRequest preferences
) {

    @NullMarked
    public record PreferencesRequest(
            @Nullable String theme,
            @Nullable String language,
            @Nullable String locale,
            @Nullable Boolean sidebarCollapsed
    ) {}
}

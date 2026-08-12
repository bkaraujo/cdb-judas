package br.cdb.feature.f001._0_domain.model;

import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f001._1_application.usecase.PreferencesPatch;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Preferências do usuário, fonte da verdade no servidor. {@code theme} é anulável:
 * {@code null} significa "não definido" — o cliente usa o tema local/sistema até a
 * primeira gravação. {@code language}/{@code locale} sempre preenchidos (padrão pt-BR);
 * registros legados/parciais são normalizados pelo construtor canônico. Novos campos
 * entram por adição (não por mapa aberto), em linha com o estilo tipado do backend.
 */
@NullMarked
public record Preferences(
        @Nullable String theme,
        String language,
        String locale,
        boolean sidebarCollapsed
) {

    public Preferences(@Nullable String theme, @Nullable String language, @Nullable String locale, boolean sidebarCollapsed) {
        this.theme = theme;
        this.language = (language == null || language.isBlank()) ? UserGuards.DEFAULT_LANGUAGE : language;
        this.locale = (locale == null || locale.isBlank()) ? UserGuards.DEFAULT_LOCALE : locale;
        this.sidebarCollapsed = sidebarCollapsed;
    }

    public static Preferences defaults() {
        return new Preferences(null, UserGuards.DEFAULT_LANGUAGE, UserGuards.DEFAULT_LOCALE, false);
    }

    /**
     * Aplica um patch parcial sobre as preferências atuais: campo nulo no patch mantém o
     * valor corrente; campo presente substitui. Lógica de domínio pura (sem efeitos).
     */
    public Preferences merge(PreferencesPatch patch) {
        return new Preferences(
                patch.theme() != null ? patch.theme() : this.theme,
                patch.language() != null ? patch.language() : this.language,
                patch.locale() != null ? patch.locale() : this.locale,
                patch.sidebarCollapsed() != null ? patch.sidebarCollapsed() : this.sidebarCollapsed
        );
    }
}

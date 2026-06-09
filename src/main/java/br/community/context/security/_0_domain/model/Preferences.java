package br.community.context.security._0_domain.model;

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

    private static final String DEFAULT_LANGUAGE = "pt-BR";
    private static final String DEFAULT_LOCALE = "pt-BR";

    public Preferences(@Nullable String theme, @Nullable String language, @Nullable String locale, boolean sidebarCollapsed) {
        this.theme = theme;
        this.language = (language == null || language.isBlank()) ? DEFAULT_LANGUAGE : language;
        this.locale = (locale == null || locale.isBlank()) ? DEFAULT_LOCALE : locale;
        this.sidebarCollapsed = sidebarCollapsed;
    }

    public static Preferences defaults() {
        return new Preferences(null, DEFAULT_LANGUAGE, DEFAULT_LOCALE, false);
    }
}

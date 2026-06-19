package br.community.core.web.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Agregado de usuário. O {@code username} é a identidade de login (imutável). O
 * {@code name} é o nome de exibição editável (anulável → cai para o username). As
 * {@code preferences} são sempre preenchidas: o construtor canônico aplica o padrão
 * quando ausentes, tolerando registros legados sem o campo.
 */
@NullMarked
public record User(
        String id,
        String username,
        @Nullable String name,
        String password,
        Preferences preferences
) {

    public User(String id, String username, @Nullable String name, String password, @Nullable Preferences preferences) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.password = password;
        this.preferences = preferences == null ? Preferences.defaults() : preferences;
    }

    /** Nome de exibição: usa o {@code name} quando presente, senão cai para o {@code username}. */
    public String displayName() {
        return (name == null || name.isBlank()) ? username : name;
    }
}

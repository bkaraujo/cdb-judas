package br.cdb.core.web.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Agregado de usuário (identidade pura). O {@code username} é a identidade de login
 * (imutável). O {@code name} é o nome de exibição editável (anulável → cai para o username).
 * {@code active} é persistido mas não bloqueia login hoje (extensão futura). As preferências do
 * usuário são uma feature e vivem fora deste agregado ({@code feature.user.profile.Preferences}).
 */
@NullMarked
public record User(
        String id,
        String username,
        @Nullable String name,
        String password,
        boolean active,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {

    public User(String id, String username, @Nullable String name, String password) {
        this(id, username, name, password, true, null, null);
    }

    /** Nome de exibição: usa o {@code name} quando presente, senão cai para o {@code username}. */
    public String displayName() {
        return (name == null || name.isBlank()) ? username : name;
    }
}

package br.cdb.core.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Agregado de usuário (identidade de login). O {@code username} é a identidade de login
 * (imutável). O {@code name} é o nome de exibição editável (anulável → cai para o username).
 * {@code active} é persistido mas não bloqueia login hoje (extensão futura). As preferências do
 * usuário são uma feature e vivem fora deste agregado ({@code feature.user.profile.Preferences}).
 *
 * <p>{@code personId} liga o login à {@code PEP_PERSON} — é a chave que todas as tabelas de dados
 * usam. A pessoa é criada a montante pelo contexto people ({@code UserService} →
 * {@code PersonUseCase.register}), antes do login; o {@code personId} é obrigatório na criação e o
 * adaptador de persistência do login nunca escreve em {@code PEP_PERSON}. Os construtores curtos
 * (sem {@code personId}) servem só à leitura/testes.</p>
 */
@NullMarked
public record User(
        String id,
        String username,
        @Nullable String name,
        String password,
        boolean active,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt,
        @Nullable String personId
) {

    public User(String id, String username, @Nullable String name, String password) {
        this(id, username, name, password, true, null, null, null);
    }

    public User(String id, String username, @Nullable String name, String password,
                boolean active, @Nullable LocalDateTime createdAt, @Nullable LocalDateTime updatedAt) {
        this(id, username, name, password, active, createdAt, updatedAt, null);
    }

    /** Nome de exibição: usa o {@code name} quando presente, senão cai para o {@code username}. */
    public String displayName() {
        return (name == null || name.isBlank()) ? username : name;
    }
}

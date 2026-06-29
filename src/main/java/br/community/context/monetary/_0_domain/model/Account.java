package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record Account(
        UUID id,
        String name,
        Type type,
        boolean active,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    public Account(UUID id, String name, Type type, boolean active) {
        this(id, name, type, active, null, null);
    }

    public enum Type {
        CHECKING,
        INVESTMENT,
        CREDIT_CARD
    }
}

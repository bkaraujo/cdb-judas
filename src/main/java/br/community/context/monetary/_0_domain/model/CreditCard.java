package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record CreditCard(
        UUID id,
        String last4,
        UUID accountId,
        boolean active,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    public CreditCard(UUID id, String last4, UUID accountId, boolean active) {
        this(id, last4, accountId, active, null, null);
    }
}

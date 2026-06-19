package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@NullMarked
public record Account(
        UUID id,
        String name,
        Type type,
        boolean active,
        @Nullable UUID linkedAccountId,
        Map<String, Object> additionalInfo,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    public Account {
        if (additionalInfo == null) additionalInfo = new HashMap<>();
    }

    public Account(UUID id, String name, Type type, boolean active, @Nullable UUID linkedAccountId) {
        this(id, name, type, active, linkedAccountId, new HashMap<>(), null, null);
    }

    public Account(UUID id, String name, Type type, boolean active, @Nullable UUID linkedAccountId, Map<String, Object> additionalInfo) {
        this(id, name, type, active, linkedAccountId, additionalInfo, null, null);
    }

    public enum Type {
        CHECKING,
        INVESTMENT,
        CREDIT_CARD
    }
}

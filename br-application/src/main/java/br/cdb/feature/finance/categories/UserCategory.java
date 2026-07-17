package br.cdb.feature.finance.categories;

import br.cdb.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record UserCategory(
        UUID id,
        UUID userId,
        Transaction.Type nature,
        String name,
        @Nullable UUID parentId,
        boolean isSystem,
        boolean active,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    public UserCategory(UUID id, UUID userId, Transaction.Type nature, String name, @Nullable UUID parentId) {
        this(id, userId, nature, name, parentId, false, true, null, null);
    }

    public UserCategory(UUID id, UUID userId, Transaction.Type nature, String name, @Nullable UUID parentId, boolean isSystem) {
        this(id, userId, nature, name, parentId, isSystem, true, null, null);
    }
}

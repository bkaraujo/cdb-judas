package br.cdb.feature.f005._0_domain;

import br.cdb.feature.f006._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record UserCategory(
        UUID id,
        UUID personId,
        Transaction.Type nature,
        String name,
        @Nullable UUID parentId,
        boolean isSystem,
        boolean active,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    public UserCategory(UUID id, UUID personId, Transaction.Type nature, String name, @Nullable UUID parentId) {
        this(id, personId, nature, name, parentId, false, true, null, null);
    }

    public UserCategory(UUID id, UUID personId, Transaction.Type nature, String name, @Nullable UUID parentId, boolean isSystem) {
        this(id, personId, nature, name, parentId, isSystem, true, null, null);
    }
}

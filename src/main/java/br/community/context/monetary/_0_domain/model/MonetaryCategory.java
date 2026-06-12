package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public record MonetaryCategory(
        UUID id,
        MonetaryTransaction.Type nature,
        String name,
        @Nullable UUID parentId,
        boolean isSystem
) {
    public MonetaryCategory(UUID id, MonetaryTransaction.Type nature, String name, @Nullable UUID parentId) {
        this(id, nature, name, parentId, false);
    }
}

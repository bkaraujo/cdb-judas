package br.cdb.feature.finance.categories;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public record CategoryResponse(
        UUID id,
        String name,
        String nature,
        @Nullable UUID parentId,
        boolean isSystem,
        boolean active
) {
    public static CategoryResponse from(UserCategory entity) {
        return new CategoryResponse(
                entity.id(),
                entity.name(),
                entity.nature().name(),
                entity.parentId(),
                entity.isSystem(),
                entity.active()
        );
    }
}

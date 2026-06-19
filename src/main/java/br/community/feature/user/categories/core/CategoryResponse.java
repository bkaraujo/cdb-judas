package br.community.feature.user.categories.core;

import br.community.context.monetary._0_domain.model.Category;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public record CategoryResponse(
        UUID id,
        String name,
        String nature,
        @Nullable UUID parentId,
        boolean isSystem
) {
    public static CategoryResponse from(Category entity) {
        return new CategoryResponse(
                entity.id(),
                entity.name(),
                entity.nature().name(),
                entity.parentId(),
                entity.isSystem()
        );
    }
}

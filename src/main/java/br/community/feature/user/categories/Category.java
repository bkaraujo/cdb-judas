package br.community.feature.user.categories;

import br.community.context.monetary._0_domain.model.MonetaryCategory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public record Category(
        UUID id,
        String name,
        String nature,
        @Nullable UUID parentId,
        boolean isSystem
) {
    public static Category from(MonetaryCategory entity) {
        return new Category(
                entity.id(),
                entity.name(),
                entity.nature().name(),
                entity.parentId(),
                entity.isSystem()
        );
    }
}

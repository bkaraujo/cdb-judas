package br.cdb.feature.f005._2_infrastructure.web.response;

import br.cdb.feature.f005._0_domain.model.Category;
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
    public static CategoryResponse from(Category entity) {
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

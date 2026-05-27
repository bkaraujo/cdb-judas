package br.community.context.monetary._0_domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MonetaryCategory(
        UUID id,
        MonetaryNature nature,
        String name,
        @Nullable UUID parentId,
        boolean isSystem
) {
    public MonetaryCategory(UUID id, MonetaryNature nature, String name, @Nullable UUID parentId) {
        this(id, nature, name, parentId, false);
    }
}

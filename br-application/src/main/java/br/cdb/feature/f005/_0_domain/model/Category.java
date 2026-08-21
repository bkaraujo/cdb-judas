package br.cdb.feature.f005._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record Category(
        UUID id,
        UUID personId,
        Nature nature,
        String name,
        @Nullable UUID parentId,
        boolean isSystem,
        boolean active,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    public Category(UUID id, UUID personId, Nature nature, String name, @Nullable UUID parentId) {
        this(id, personId, nature, name, parentId, false, true, null, null);
    }

    public Category(UUID id, UUID personId, Nature nature, String name, @Nullable UUID parentId, boolean isSystem) {
        this(id, personId, nature, name, parentId, isSystem, true, null, null);
    }
}

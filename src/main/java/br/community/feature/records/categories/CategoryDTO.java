package br.community.feature.records.categories;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public record CategoryDTO(
        UUID id,
        String name,
        String nature,
        @Nullable UUID parentId,
        boolean isSystem
) {}

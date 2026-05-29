package br.community.feature.user.categories;

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
) {}

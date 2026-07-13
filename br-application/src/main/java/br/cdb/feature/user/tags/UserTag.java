package br.cdb.feature.user.tags;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record UserTag(
        UUID id,
        UUID userId,
        String name,
        String color,
        @Nullable LocalDateTime createdAt
) {}

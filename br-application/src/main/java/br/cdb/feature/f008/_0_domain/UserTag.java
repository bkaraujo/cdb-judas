package br.cdb.feature.f008._0_domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record UserTag(
        UUID id,
        UUID personId,
        String name,
        String color,
        @Nullable LocalDateTime createdAt
) {}

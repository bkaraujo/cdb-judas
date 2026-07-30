package br.cdb.feature.f004._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record Tag(
        UUID id,
        UUID personId,
        String name,
        String color,
        @Nullable LocalDateTime createdAt
) {}

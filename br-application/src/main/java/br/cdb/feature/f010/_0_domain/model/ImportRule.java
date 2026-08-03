package br.cdb.feature.f010._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record ImportRule(
        UUID id,
        UUID personId,
        String name,
        @Nullable UUID accountId,
        @Nullable UUID categoryId,
        @Nullable UUID costCenterId,
        @Nullable LocalDateTime createdAt
) {}

package br.cdb.feature.f006._0_domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record UserTransaction(
        UUID transactionId,
        UUID personId,
        UUID accountId,
        @Nullable UUID categoryId,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {}

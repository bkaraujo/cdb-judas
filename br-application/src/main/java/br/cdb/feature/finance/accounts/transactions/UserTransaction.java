package br.cdb.feature.finance.accounts.transactions;

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

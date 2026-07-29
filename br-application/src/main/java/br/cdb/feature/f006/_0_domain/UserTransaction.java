package br.cdb.feature.f006._0_domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vínculo transação↔categoria ({@code F005_TRANSACTION_CATEGORY}). Conta e pessoa da transação em si
 * já são colunas nativas de {@code F006_TRANSACTION} desde a fusão dos contextos — este registro
 * carrega só o que ainda é overlay: a categoria.
 */
@NullMarked
public record UserTransaction(
        UUID transactionId,
        UUID personId,
        @Nullable UUID categoryId,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {}

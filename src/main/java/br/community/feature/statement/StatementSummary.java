package br.community.feature.statement;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.UUID;

@NullMarked
public record StatementSummary(
        UUID accountId,
        String accountName,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal totalIn,
        BigDecimal totalOut
) {}

package br.community.feature.user.accounts.statement;

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

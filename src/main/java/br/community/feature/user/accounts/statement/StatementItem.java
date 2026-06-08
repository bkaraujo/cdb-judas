package br.community.feature.user.accounts.statement;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NullMarked
public record StatementItem(
        LocalDate date,
        String description,
        BigDecimal amount,
        String status,
        BigDecimal runningBal,
        @Nullable UUID categoryId
) {}

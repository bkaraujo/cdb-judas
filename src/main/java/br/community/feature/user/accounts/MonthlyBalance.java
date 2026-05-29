package br.community.feature.user.accounts;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@NullMarked
public record MonthlyBalance(
        UUID id,
        UUID accountId,
        YearMonth period,
        BigDecimal balance
) {}

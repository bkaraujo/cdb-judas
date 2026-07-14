package br.cdb.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@NullMarked
public record AccountBalance(
        UUID accountId,
        YearMonth period,
        BigDecimal balance
) {}

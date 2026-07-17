package br.cdb.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.YearMonth;

@NullMarked
public record Balance(
        Account account,
        YearMonth period,
        BigDecimal value
) {}

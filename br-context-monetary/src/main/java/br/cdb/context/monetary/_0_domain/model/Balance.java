package br.cdb.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;

@NullMarked
public record Balance (
        LocalDate date,
        BigDecimal amount
) {}

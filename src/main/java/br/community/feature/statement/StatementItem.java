package br.community.feature.statement;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;

@NullMarked
public record StatementItem(
        LocalDate date,
        String description,
        BigDecimal amount,
        String status,
        BigDecimal runningBal
) {}

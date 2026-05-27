package br.community.feature.operations.payables;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NullMarked
public record AccountPayable(
        UUID id,
        String name,
        LocalDate due,
        BigDecimal amount,
        UUID accountId,
        UUID categoryId,
        String status,
        String type
) {}

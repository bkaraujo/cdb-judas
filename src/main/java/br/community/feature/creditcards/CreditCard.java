package br.community.feature.creditcards;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.UUID;

@NullMarked
public record CreditCard(
        UUID id,
        UUID accountId,
        String last4,
        BigDecimal limit,
        int closingDay,
        int dueDay,
        String color,
        boolean active
) {}

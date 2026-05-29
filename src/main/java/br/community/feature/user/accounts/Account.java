package br.community.feature.user.accounts;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@NullMarked
public record Account(
        UUID id,
        String name,
        BigDecimal balance,
        String type,
        String color,
        boolean active,
        @Nullable UUID linkedAccountId,
        Map<String, Object> additionalInfo,
        BigDecimal currentBalance
) {}

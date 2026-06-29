package br.community.feature.user.accounts.core;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Overlay por utilizador de uma conta: saldo de abertura, cor, estado activo e os dados de cartão
 * (conta vinculada para determinação de cartão, last4/dia de vencimento/fecho) e limites de uso
 * — limite de crédito do cartão e limite de "cheque especial".
 */
@NullMarked
public record UserAccount(
        String userId,
        UUID accountId,
        BigDecimal openingBalance,
        String color,
        boolean active,
        @Nullable UUID linkedAccountId,
        @Nullable String last4,
        @Nullable Integer dueDay,
        @Nullable Integer closingDay,
        @Nullable BigDecimal creditLimit,
        @Nullable BigDecimal overdraftLimit
) {
    /** Overlay simples (não-cartão): sem conta vinculada, sem dados de cartão nem limites. */
    public UserAccount(String userId, UUID accountId, BigDecimal openingBalance, String color, boolean active) {
        this(userId, accountId, openingBalance, color, active, null, null, null, null, null, null);
    }
}

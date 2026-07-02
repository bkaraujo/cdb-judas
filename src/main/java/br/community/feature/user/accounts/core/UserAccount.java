package br.community.feature.user.accounts.core;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.UUID;

/** Overlay por utilizador de uma conta: saldo de abertura, cor e estado ativo. */
@NullMarked
public record UserAccount(
        String userId,
        UUID accountId,
        BigDecimal openingBalance,
        String color,
        boolean active
) {}

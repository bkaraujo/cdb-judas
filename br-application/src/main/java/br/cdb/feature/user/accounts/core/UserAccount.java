package br.cdb.feature.user.accounts.core;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Overlay por utilizador de uma conta: só a cor. Saldo e estado ativo vêm do contexto monetário
 * ({@code Account}) — saldo inicial histórico virou transação normal na migração.
 */
@NullMarked
public record UserAccount(
        String userId,
        UUID accountId,
        String color
) {}

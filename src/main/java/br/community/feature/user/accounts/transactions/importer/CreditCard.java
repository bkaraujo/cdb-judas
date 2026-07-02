package br.community.feature.user.accounts.transactions.importer;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Visão (feature) de um cartão de crédito para a importação de faturas: combina a identidade do
 * cartão do contexto monetário ({@code id}, {@code last4}) com a conta real a que pertence
 * ({@code accountId}, {@code accountName}) — é nela que as transações do cartão são persistidas.
 */
@NullMarked
public record CreditCard(
        UUID id,
        UUID accountId,
        String accountName,
        String last4
) {}

package br.community.feature.user.accounts.transactions.importer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Visão (feature) de um cartão de crédito para a importação de faturas: combina a identidade da conta
 * monetária ({@code id}, {@code name}) com os dados do overlay {@code USER_ACCOUNT} (last4 e a conta
 * vinculada que paga a fatura). Os dados de cartão pertencem à feature, não ao contexto monetary.
 */
@NullMarked
public record CreditCard(
        UUID id,
        String name,
        @Nullable String last4,
        @Nullable UUID linkedAccountId
) {}

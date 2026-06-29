package br.community.feature.user.accounts.transactions.importer;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Fornece os cartões de crédito do utilizador corrente para a importação de faturas. Os dados de
 * cartão (last4, conta vinculada) pertencem ao overlay de feature {@code USER_ACCOUNT}; esta porta
 * mantém o {@link StatementImportUseCase} desacoplado da origem (e testável com um fake).
 */
@FunctionalInterface
@NullMarked
public interface CreditCardProvider {

    /** Cartões de crédito do utilizador corrente (com last4 e conta vinculada, quando houver). */
    List<CreditCard> creditCards();
}

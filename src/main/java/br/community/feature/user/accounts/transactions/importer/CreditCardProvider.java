package br.community.feature.user.accounts.transactions.importer;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Fornece os cartões de crédito (entidade do contexto monetário) para a importação de faturas. Esta
 * porta mantém o {@link StatementImportUseCase} desacoplado da origem (e testável com um fake).
 */
@FunctionalInterface
@NullMarked
public interface CreditCardProvider {

    /** Todos os cartões cadastrados, com a conta real a que pertencem. */
    List<CreditCard> creditCards();
}

package br.cdb.feature.user.accounts.transactions.importer;

import br.cdb.context.monetary._0_domain.model.CreditCard;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Fornece os cartões de crédito (entidade do contexto monetário) para a importação de faturas. Esta
 * porta mantém o {@link StatementImportUseCase} desacoplado da origem (e testável com um fake).
 */
@FunctionalInterface
@NullMarked
public interface CreditCardProvider {

    /** Todos os cartões cadastrados. */
    List<CreditCard> creditCards();
}

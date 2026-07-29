package br.cdb.feature.f007._0_domain;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f007._1_application.StatementImportService;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Fornece os cartões de crédito (entidade do contexto monetário) para a importação de faturas. Esta
 * porta mantém o {@link StatementImportService} desacoplado da origem (e testável com um fake).
 */
@FunctionalInterface
@NullMarked
public interface CreditCardProvider {

    /** Todos os cartões cadastrados. */
    List<CreditCard> creditCards();
}

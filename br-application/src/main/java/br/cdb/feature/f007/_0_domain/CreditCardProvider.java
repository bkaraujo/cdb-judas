package br.cdb.feature.f007._0_domain;

import br.cdb.feature.f003.F003Api;
import br.cdb.feature.f007._1_application.service.StatementImportService;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Fornece os cartões de crédito para a importação de faturas. Esta porta mantém o
 * {@link StatementImportService} desacoplado da origem (e testável com um fake). Projeção
 * {@link F003Api.CardView}, não o modelo {@code CreditCard} de {@code f003} (fatia irmã): leitura
 * cross-slice é sempre via {@code F003Api} (D1 de {@code .claude/plan.md}).
 */
@FunctionalInterface
@NullMarked
public interface CreditCardProvider {

    /** Todos os cartões cadastrados. */
    List<F003Api.CardView> creditCards();
}

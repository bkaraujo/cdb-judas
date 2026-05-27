package br.community.context.monetary._1_application.service;

import br.community.context.monetary._0_domain.model.ParsedStatement;
import org.jspecify.annotations.NullMarked;

/**
 * Turns the extracted text of one bank's credit-card statement into a {@link ParsedStatement}.
 *
 * <p>Charge-vs-skip classification lives inside each implementation because the wording and layout
 * are bank-specific: purchases, IOF, anuidade, juros and multa are kept; payment-received
 * (pagamento da fatura anterior) and summary / previous-balance lines are dropped.
 */
@NullMarked
public interface CreditCardStatementParser {

    ParsedStatement parse(String text);
}

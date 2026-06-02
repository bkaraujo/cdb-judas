package br.community.feature.user.accounts.statementimport.preview;

import br.commons.tools.Parser;
import org.jspecify.annotations.NullMarked;

/**
 * Turns the extracted text of one bank's checking-account statement into a {@link ParsedBankStatement}.
 *
 * <p>Keep-vs-drop classification lives inside each implementation because the wording and layout are
 * bank-specific: real movements (Pix, boletos, débito, IOF/juros, investimentos) are kept with their
 * signed amount; running-balance lines and the aggregate card-invoice payment are dropped (the latter
 * because the credit-card invoice import already posts those charges individually).
 */
@NullMarked
public interface BankStatementParser extends Parser<ParsedBankStatement> {

    ParsedBankStatement parse(String text);
}

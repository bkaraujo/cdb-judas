package br.community.feature.user.accounts.statement.importer.provider;

import br.commons.tools.Strings;
import br.community.feature.user.accounts.statement.Amounts;
import br.community.feature.user.accounts.statement.importer.StatementParser;
import br.community.feature.user.accounts.statement.importer.preview.ChargeKind;
import br.community.feature.user.accounts.statement.importer.preview.Issuer;
import br.community.feature.user.accounts.statement.importer.MonetaryDocument;
import br.community.feature.user.accounts.statement.importer.preview.ParsedStatementLine;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.commons.chrono.Dates.MONTHS_ENUS;

/**
 * BTG Pactual statement parser.
 *
 * <p>BTG prints the amount first, then the description, an optional {@code (n/N)} installment marker
 * and a year-less {@code DD Mon} date — e.g. {@code R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul}.
 * Charge-vs-skip is driven by section + sub-header, not per-line wording: each physical/virtual card
 * block (headed by {@code … Final NNNN Total do cartão …}) keeps the lines under its
 * {@code Total de compras e despesas} sub-header and drops everything else — credits
 * ({@code Total de créditos recebidos}), payments, fatura financing ({@code Parcelamento de fatura}),
 * the {@code Resumo da Fatura Atual} block and the annuity ({@code Mensalidade}). The card subtotal
 * line (an amount with nothing else) closes the kept block.
 *
 * <p>International (US$) lines are not emitted: the BRL amount is not on the line, so this slice
 * keeps only the R$ charges.
 */
@NullMarked
public class BTGInvoiceParser implements StatementParser {

    private static final Pattern CARD_HEADER = Pattern.compile("Final (\\d{4}) Total do cart[ãa]o");
    private static final Pattern SUBTOTAL = Pattern.compile("^-?\\s*(?:R\\$|US\\$)\\s*[\\d.]+,\\d{2}\\s*$");
    private static final Pattern TXN = Pattern.compile(
            "^\\s*(R\\$|US\\$)\\s*([\\d.]+,\\d{2})(.+?)(?:\\((\\d+)/(\\d+)\\))?(\\d{2}) ([A-Za-z]{3})$");

    private static final String KEEP_HEADER = "Total de compras e despesas";
    private static final String CREDIT_HEADER = "Total de créditos recebidos";

    private static final String CARD_CNPJ_DIGITS = "30306294000145";

    @Override
    public boolean parseable(String raw) {
        val text = new DocumentText(raw);
        return !BankStatements.isAny(text)
                && (text.hasCnpj(CARD_CNPJ_DIGITS) || text.nameOnly("BTG PACTUAL", "SANTANDER"));
    }

    @Override
    public MonetaryDocument parse(String text) {
        val lines = new ArrayList<ParsedStatementLine>();
        String last4 = null;
        boolean keep = false;

        for (val line : text.split("\\R", -1)) {
            val card = CARD_HEADER.matcher(line);
            if (card.find()) {
                last4 = card.group(1);
                keep = false;
                continue;
            }

            val section = sectionKeep(line);
            if (section != null) {
                keep = section;
                continue;
            }

            if (!keep || last4 == null) {
                continue;
            }

            val txn = txnLine(line, last4);
            if (txn != null) {
                lines.add(txn);
            }
        }

        return new MonetaryDocument.Invoice(Issuer.BTG, List.copyOf(lines));
    }

    /** {@code true}/{@code false} se a linha alterna a seção de interesse; {@code null} caso contrário. */
    private static @Nullable Boolean sectionKeep(String line) {
        val trimmed = line.trim();
        if (trimmed.equals(KEEP_HEADER)) {
            return Boolean.TRUE;
        }
        if (trimmed.equals(CREDIT_HEADER) || SUBTOTAL.matcher(line).matches()) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static @Nullable ParsedStatementLine txnLine(String line, String last4) {
        val m = TXN.matcher(line);
        if (!m.matches() || m.group(1).equals("US$")) {
            return null;
        }
        val month = month(m.group(7));
        if (month == 0) {
            return null;
        }
        val date = MonthDay.of(month, Integer.parseInt(m.group(6)));
        val number = m.group(4) == null ? null : Integer.valueOf(m.group(4));
        val total = m.group(5) == null ? null : Integer.valueOf(m.group(5));
        return ParsedStatementLine.charge(
                last4, date, m.group(3).trim(), Amounts.brl(m.group(2)), number, total, ChargeKind.PURCHASE);
    }

    private static int month(String abbr) {
        val upper = Strings.upper(abbr);
        for (val month : MONTHS_ENUS.keySet()) {
            if (month.startsWith(upper)) {
                return MONTHS_ENUS.get(month);
            }
        }

        return 0;
    }
}

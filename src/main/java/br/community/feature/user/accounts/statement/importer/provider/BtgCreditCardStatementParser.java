package br.community.feature.user.accounts.statement.importer.provider;

import br.community.feature.user.accounts.statement.importer.preview.ChargeKind;
import br.community.feature.user.accounts.statement.importer.preview.CreditCardStatementParser;
import br.community.feature.user.accounts.statement.importer.preview.ParsedStatement;
import br.community.feature.user.accounts.statement.importer.preview.ParsedStatementLine;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class BtgCreditCardStatementParser implements CreditCardStatementParser {

    private static final Pattern CARD_HEADER = Pattern.compile("Final (\\d{4}) Total do cart[ãa]o");
    private static final Pattern SUBTOTAL = Pattern.compile("^-?\\s*(?:R\\$|US\\$)\\s*[\\d.]+,\\d{2}\\s*$");
    private static final Pattern TXN = Pattern.compile(
            "^\\s*(R\\$|US\\$)\\s*([\\d.]+,\\d{2})(.+?)(?:\\((\\d+)/(\\d+)\\))?(\\d{2}) ([A-Za-z]{3})$");

    private static final String KEEP_HEADER = "Total de compras e despesas";
    private static final String CREDIT_HEADER = "Total de créditos recebidos";

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("fev", 2), Map.entry("mar", 3), Map.entry("abr", 4),
            Map.entry("mai", 5), Map.entry("jun", 6), Map.entry("jul", 7), Map.entry("ago", 8),
            Map.entry("set", 9), Map.entry("out", 10), Map.entry("nov", 11), Map.entry("dez", 12));

    @Override
    public ParsedStatement parse(String text) {
        final List<ParsedStatementLine> lines = new ArrayList<>();
        @Nullable String last4 = null;
        boolean keep = false;

        for (String line : text.split("\\R", -1)) {
            final Matcher card = CARD_HEADER.matcher(line);
            if (card.find()) {
                last4 = card.group(1);
                keep = false;
                continue;
            }

            final Boolean section = sectionKeep(line);
            if (section != null) {
                keep = section;
                continue;
            }
            if (!keep || last4 == null) {
                continue;
            }
            final ParsedStatementLine txn = txnLine(line, last4);
            if (txn != null) {
                lines.add(txn);
            }
        }

        return new ParsedStatement(List.copyOf(lines));
    }

    /** {@code true}/{@code false} se a linha alterna a seção de interesse; {@code null} caso contrário. */
    private static @Nullable Boolean sectionKeep(String line) {
        final String trimmed = line.trim();
        if (trimmed.equals(KEEP_HEADER)) {
            return Boolean.TRUE;
        }
        if (trimmed.equals(CREDIT_HEADER) || SUBTOTAL.matcher(line).matches()) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static @Nullable ParsedStatementLine txnLine(String line, String last4) {
        final Matcher m = TXN.matcher(line);
        if (!m.matches() || m.group(1).equals("US$")) {
            return null;
        }
        final int month = month(m.group(7));
        if (month == 0) {
            return null;
        }
        final MonthDay date = MonthDay.of(month, Integer.parseInt(m.group(6)));
        final Integer number = m.group(4) == null ? null : Integer.valueOf(m.group(4));
        final Integer total = m.group(5) == null ? null : Integer.valueOf(m.group(5));
        return new ParsedStatementLine(
                last4, date, m.group(3).trim(), Amounts.brl(m.group(2)), number, total, ChargeKind.PURCHASE);
    }

    private static int month(String abbr) {
        return MONTHS.getOrDefault(abbr.toLowerCase(Locale.ROOT), 0);
    }
}

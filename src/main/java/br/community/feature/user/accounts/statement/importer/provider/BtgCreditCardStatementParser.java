package br.community.feature.user.accounts.statement.importer.provider;

import br.community.feature.user.accounts.statement.importer.preview.ChargeKind;
import br.community.feature.user.accounts.statement.importer.preview.CreditCardStatementParser;
import br.community.feature.user.accounts.statement.importer.preview.ParsedStatement;
import br.community.feature.user.accounts.statement.importer.preview.ParsedStatementLine;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

            final String trimmed = line.trim();
            if (trimmed.equals(KEEP_HEADER)) {
                keep = true;
                continue;
            }
            if (trimmed.equals(CREDIT_HEADER)) {
                keep = false;
                continue;
            }
            if (SUBTOTAL.matcher(line).matches()) {
                keep = false;
                continue;
            }
            if (!keep || last4 == null) {
                continue;
            }

            final Matcher m = TXN.matcher(line);
            if (!m.matches() || m.group(1).equals("US$")) {
                continue;
            }
            final int month = month(m.group(7));
            if (month == 0) {
                continue;
            }

            final MonthDay date = MonthDay.of(month, Integer.parseInt(m.group(6)));
            final Integer number = m.group(4) == null ? null : Integer.valueOf(m.group(4));
            final Integer total = m.group(5) == null ? null : Integer.valueOf(m.group(5));
            lines.add(new ParsedStatementLine(
                    last4, date, m.group(3).trim(), amount(m.group(2)), number, total, ChargeKind.PURCHASE));
        }

        return new ParsedStatement(List.copyOf(lines));
    }

    private static BigDecimal amount(String raw) {
        return new BigDecimal(raw.replace(".", "").replace(",", "."));
    }

    private static int month(String abbr) {
        return switch (abbr.toLowerCase(Locale.ROOT)) {
            case "jan" -> 1;
            case "fev" -> 2;
            case "mar" -> 3;
            case "abr" -> 4;
            case "mai" -> 5;
            case "jun" -> 6;
            case "jul" -> 7;
            case "ago" -> 8;
            case "set" -> 9;
            case "out" -> 10;
            case "nov" -> 11;
            case "dez" -> 12;
            default -> 0;
        };
    }
}

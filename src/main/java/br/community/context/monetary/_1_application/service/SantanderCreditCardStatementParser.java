package br.community.context.monetary._1_application.service;

import br.community.context.monetary._0_domain.model.ChargeKind;
import br.community.context.monetary._0_domain.model.ParsedStatement;
import br.community.context.monetary._0_domain.model.ParsedStatementLine;
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
 * Santander statement parser.
 *
 * <p>Santander prints an optional 1-digit channel code, a year-less {@code DD/MM} date, the
 * description, an optional {@code NN/NN} parcela column and the BRL value (with an optional US$
 * value) — e.g. {@code 3 03/08 DECATHLON 08/10 111,98}. Charge-vs-skip is section-header + sign
 * driven: {@code Parcelamentos} and {@code Despesas} are kept (the latter includes {@code ANUIDADE}
 * and the foreign {@code IOF DESPESA NO EXTERIOR}); {@code Pagamento e Demais Créditos} and
 * {@code Resumo da Fatura} are dropped, as is any negative amount (refund / credit). Each card's
 * masked PAN header ({@code NNNN XXXX XXXX NNNN}) tags the following lines with that card's last4.
 *
 * <p>The foreign IOF line carries no date of its own; it is anchored to the most recent kept
 * charge, which is the foreign purchase it follows.
 */
@NullMarked
public class SantanderCreditCardStatementParser implements CreditCardStatementParser {

    private static final Pattern CARD_HEADER = Pattern.compile("\\d{4}\\s+X{4}\\s+X{4}\\s+(\\d{4})");
    private static final Pattern TXN = Pattern.compile(
            "^\\s*(?:\\d\\s+)?(\\d{2})/(\\d{2})\\s+(.+?)(?:\\s+(\\d{2})/(\\d{2}))?\\s+(-?[\\d.]+,\\d{2})(?:\\s+-?[\\d.]+,\\d{2})?\\s*$");
    private static final Pattern IOF_LINE = Pattern.compile("^(IOF\\b.*?)\\s+(-?[\\d.]+,\\d{2})$");

    @Override
    public ParsedStatement parse(String text) {
        final List<ParsedStatementLine> lines = new ArrayList<>();
        @Nullable String last4 = null;
        @Nullable MonthDay lastDate = null;
        boolean keep = false;

        for (String line : text.split("\\R", -1)) {
            final Matcher card = CARD_HEADER.matcher(line);
            if (card.find()) {
                last4 = card.group(1);
                keep = false;
                continue;
            }

            final String trimmed = line.trim();
            if (trimmed.equals("Parcelamentos") || trimmed.equals("Despesas")) {
                keep = true;
                continue;
            }
            if (trimmed.equals("Pagamento e Demais Créditos") || trimmed.equals("Resumo da Fatura")
                    || trimmed.startsWith("VALOR TOTAL")) {
                keep = false;
                continue;
            }
            if (!keep || last4 == null) {
                continue;
            }

            final Matcher iof = IOF_LINE.matcher(trimmed);
            if (iof.matches()) {
                final BigDecimal value = amount(iof.group(2));
                if (value.signum() >= 0 && lastDate != null) {
                    lines.add(new ParsedStatementLine(
                            last4, lastDate, iof.group(1).trim(), value, null, null, ChargeKind.IOF));
                }
                continue;
            }

            final Matcher m = TXN.matcher(line);
            if (!m.matches()) {
                continue;
            }
            final BigDecimal value = amount(m.group(6));
            if (value.signum() < 0) {
                continue;
            }

            final MonthDay date = MonthDay.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            final Integer number = m.group(4) == null ? null : Integer.valueOf(m.group(4));
            final Integer total = m.group(5) == null ? null : Integer.valueOf(m.group(5));
            final String description = m.group(3).trim();
            lines.add(new ParsedStatementLine(
                    last4, date, description, value, number, total, classify(description)));
            lastDate = date;
        }

        return new ParsedStatement(List.copyOf(lines));
    }

    private static ChargeKind classify(String description) {
        return description.toUpperCase(Locale.ROOT).startsWith("ANUIDADE") ? ChargeKind.FEE : ChargeKind.PURCHASE;
    }

    private static BigDecimal amount(String raw) {
        return new BigDecimal(raw.replace(".", "").replace(",", "."));
    }
}

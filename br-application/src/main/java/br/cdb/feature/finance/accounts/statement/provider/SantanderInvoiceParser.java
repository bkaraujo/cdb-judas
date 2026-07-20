package br.cdb.feature.finance.accounts.statement.provider;

import br.cdb.feature.finance.accounts.statement.MonetaryDocument;
import br.cdb.feature.finance.accounts.statement.MonetaryDocumentEntry;
import br.cdb.feature.finance.accounts.statement.StatementParser;
import br.cdb.feature.finance.accounts.transactions.core.ChargeKind;
import br.cdb.feature.finance.accounts.transactions.importer.Amounts;
import br.commons.Logger;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
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
public class SantanderInvoiceParser implements StatementParser {

    private static final Pattern CARD_HEADER = Pattern.compile("\\d{4}\\s+X{4}\\s+X{4}\\s+(\\d{4})");
    private static final Pattern TXN = Pattern.compile(
            "^\\s*(?:\\d\\s+)?(\\d{2})/(\\d{2})\\s+(.+?)(?:\\s+(\\d{2})/(\\d{2}))?\\s+(-?[\\d.]+,\\d{2})(?:\\s+-?[\\d.]+,\\d{2})?\\s*$");
    private static final Pattern IOF_LINE = Pattern.compile("^(IOF\\b.*?)\\s+(-?[\\d.]+,\\d{2})$");
    private static final String CNPJ_DIGITS = "90400888000142";

    @Override
    public boolean parseable(String raw) {
        val text = new DocumentText(raw);
        return !BankStatements.isAny(text)
                && (text.hasCnpj(CNPJ_DIGITS) || text.nameOnly("SANTANDER", "BTG PACTUAL"));
    }

    @Override
    public MonetaryDocument parse(String text) {
        Logger.debug("Parsing Invoice");
        val lines = new ArrayList<MonetaryDocumentEntry>();
        @Nullable String last4 = null;
        @Nullable MonthDay lastDate = null;
        boolean keep = false;

        for (val line : text.split("\\R", -1)) {
            val card = CARD_HEADER.matcher(line);
            if (card.find()) {
                last4 = card.group(1);
                keep = false;
                continue;
            }

            val trimmed = line.trim();
            val section = sectionKeep(trimmed);
            if (section != null) {
                keep = section;
                continue;
            }
            if (!keep || last4 == null) {
                continue;
            }
            lastDate = emitLine(line, trimmed, last4, lastDate, lines);
        }

        return new MonetaryDocument.Invoice("Santander", List.copyOf(lines));
    }

    /** {@code true}/{@code false} se a linha alterna a seção de interesse; {@code null} caso contrário. */
    private static @Nullable Boolean sectionKeep(String trimmed) {
        if (trimmed.equals("Parcelamentos") || trimmed.equals("Despesas")) {
            return Boolean.TRUE;
        }
        if (trimmed.equals("Pagamento e Demais Créditos") || trimmed.equals("Resumo da Fatura")
                || trimmed.startsWith("VALOR TOTAL")) {
            return Boolean.FALSE;
        }
        return null;
    }

    /** Emite a linha IOF ou de transação (se houver) e devolve a data corrente que ancora o IOF. */
    private static @Nullable MonthDay emitLine(String line, String trimmed, String last4,
                                               @Nullable MonthDay lastDate, List<MonetaryDocumentEntry> lines) {
        val iof = iofLine(trimmed, last4, lastDate);
        if (iof != null) {
            lines.add(iof);
            return lastDate;
        }
        val txn = txnLine(line, last4);
        if (txn != null) {
            lines.add(txn);
            return MonthDay.from(txn.date());
        }
        return lastDate;
    }

    /** A linha de IOF não traz data própria — é ancorada na última transação (a compra no exterior). */
    private static @Nullable MonetaryDocumentEntry iofLine(String trimmed, String last4, @Nullable MonthDay lastDate) {
        val iof = IOF_LINE.matcher(trimmed);
        if (!iof.matches()) {
            return null;
        }
        val value = Amounts.brl(iof.group(2));
        if (value.signum() < 0 || lastDate == null) {
            return null;
        }
        return MonetaryDocumentEntry.charge(last4, lastDate, iof.group(1).trim(), value, ChargeKind.IOF);
    }

    private static @Nullable MonetaryDocumentEntry txnLine(String line, String last4) {
        val m = TXN.matcher(line);
        if (!m.matches()) {
            return null;
        }
        val value = Amounts.brl(m.group(6));
        if (value.signum() < 0) {
            return null;
        }
        val date = MonthDay.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
        val number = m.group(4) == null ? 1 : Integer.parseInt(m.group(4));
        val total = m.group(5) == null ? 1 : Integer.parseInt(m.group(5));
        val description = m.group(3).trim();
        return MonetaryDocumentEntry.charge(last4, date, description, value, number, total, classify(description));
    }

    private static ChargeKind classify(String description) {
        return Strings.upper(description).startsWith("ANUIDADE") ? ChargeKind.FEE : ChargeKind.PURCHASE;
    }
}

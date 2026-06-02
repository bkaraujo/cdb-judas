package br.community.feature.user.accounts.statementimport.provider;

import br.community.feature.user.accounts.statementimport.preview.ParsedBankStatementLine;
import br.community.feature.user.accounts.statementimport.preview.BankStatementParser;
import br.community.feature.user.accounts.statementimport.preview.ParsedBankStatement;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BTG Pactual checking-account statement parser.
 *
 * <p>PDFBox emits the table as one logical record per movement, but a record may span several
 * physical lines when the {@code Categoria} column wraps (e.g. {@code "Impostos e" / "Tributos"} or
 * {@code "Contas" / "Pagamento de fatura do cartão"}) or when the description wraps. Each record is
 * anchored by a {@code dd/MM/yyyy HHhMM} prefix and closed by the line carrying its {@code R$} value
 * (debits print {@code -R$}, credits {@code R$}); everything in between is the category/transação/
 * descrição text.
 *
 * <p>Dropped: {@code Saldo Diário}/{@code Saldo final} running balances, the {@code Lançamentos:}
 * header line (its value is the period's closing balance, not a movement) and any {@code Pagamento de
 * fatura do cartão} aggregate (already covered, item by item, by the credit-card invoice import).
 */
@NullMarked
public class BtgBankStatementParser implements BankStatementParser {

    private static final Pattern DATE_PREFIX =
            Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})\\s+\\d{2}h\\d{2}\\b(.*)$");
    private static final Pattern TRAILING_VALUE =
            Pattern.compile("(-)?\\s*R\\$\\s*([\\d.]+,\\d{2})\\s*$");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    @Override
    public ParsedBankStatement parse(String text) {
        final List<ParsedBankStatementLine> lines = new ArrayList<>();

        LocalDate recordDate = null;
        final StringBuilder buffer = new StringBuilder();

        for (String raw : text.split("\\R", -1)) {
            final String line = raw.trim();
            if (line.isEmpty() || line.regionMatches(true, 0, "Lançamentos", 0, "Lançamentos".length())) {
                continue;
            }

            final Matcher date = DATE_PREFIX.matcher(line);
            if (date.find()) {
                // A new record begins; the previous one is abandoned if it never reached a value
                // (page headers between the print-date line and the first real movement).
                recordDate = LocalDate.parse(date.group(1), DATE);
                buffer.setLength(0);
                appendPart(buffer, date.group(2).trim());
                if (TRAILING_VALUE.matcher(buffer).find()) {
                    finalize(recordDate, buffer.toString(), lines);
                    recordDate = null;
                }
                continue;
            }

            if (recordDate == null) {
                continue; // header/footer noise outside any record
            }

            appendPart(buffer, line);
            if (TRAILING_VALUE.matcher(buffer).find()) {
                finalize(recordDate, buffer.toString(), lines);
                recordDate = null;
            }
        }

        return new ParsedBankStatement(List.copyOf(lines));
    }

    private static void finalize(LocalDate date, String record, List<ParsedBankStatementLine> out) {
        final Matcher value = TRAILING_VALUE.matcher(record);
        if (!value.find()) {
            return;
        }
        final String description = record.substring(0, value.start()).replaceAll("\\s+", " ").trim();
        if (isDropped(description)) {
            return;
        }
        BigDecimal amount = new BigDecimal(value.group(2).replace(".", "").replace(",", "."));
        if (value.group(1) != null) {
            amount = amount.negate();
        }
        out.add(new ParsedBankStatementLine(date, description, amount));
    }

    private static boolean isDropped(String description) {
        return description.isEmpty()
                || description.contains("Saldo Diário")
                || description.contains("Saldo final")
                || description.contains("Pagamento de fatura do cartão");
    }

    private static void appendPart(StringBuilder buffer, String part) {
        if (part.isEmpty()) {
            return;
        }
        if (!buffer.isEmpty()) {
            buffer.append(' ');
        }
        buffer.append(part);
    }
}

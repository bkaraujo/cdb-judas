package br.cdb.feature.finance.accounts.statement.provider;

import br.cdb.feature.finance.accounts.statement.Issuer;
import br.cdb.feature.finance.accounts.statement.MonetaryDocument;
import br.cdb.feature.finance.accounts.statement.MonetaryDocumentEntry;
import br.cdb.feature.finance.accounts.statement.StatementParser;
import br.cdb.feature.finance.accounts.transactions.importer.Amounts;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * header line (its value is the period's closing value, not a movement) and any {@code Pagamento de
 * fatura do cartão} aggregate (already covered, item by item, by the credit-card invoice import).
 */
@NullMarked
public class BTGStatementParser implements StatementParser {

    private static final Pattern DATE_PREFIX = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})\\s+\\d{2}h\\d{2}\\b(.*)$");
    private static final Pattern TRAILING_VALUE = Pattern.compile("(-)?\\s*R\\$\\s*([\\d.]+,\\d{2})\\s*$");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    @Override
    public boolean parseable(String raw) {
        return looksLikeStatement(new DocumentText(raw));
    }

    /** BTG's checking-account CNPJ (…0002-26), distinct from the card-issuer CNPJ (…0001-45) on the invoice. */
    private static final String ACCOUNT_CNPJ = "30306294000226";

    /** BTG's own checking-account header, its Saldo Diário/Data e hora columns, or its account CNPJ —
     *  each identifies the extrato on its own (the card-issuer CNPJ does not, and is left to the invoice). */
    static boolean looksLikeStatement(DocumentText text) {
        return text.has("EXTRATO DA SUA CONTA CORRENTE BTG PACTUAL")
                || (text.has("SALDO DIÁRIO") && text.has("DATA E HORA"))
                || text.hasCnpj(ACCOUNT_CNPJ);
    }

    @Override
    public MonetaryDocument parse(String text) {
        val lines = new ArrayList<MonetaryDocumentEntry>();

        LocalDate recordDate = null;
        val buffer = new StringBuilder();

        for (val raw : text.split("\\R", -1)) {
            val line = raw.trim();
            if (line.isEmpty() || line.regionMatches(true, 0, "Lançamentos", 0, "Lançamentos".length())) {
                continue;
            }

            val date = DATE_PREFIX.matcher(line);
            if (date.find()) {
                // A new record begins; the previous one is abandoned if it never reached a value
                // (page headers between the print-date line and the first real movement).
                recordDate = LocalDate.parse(date.group(1), DATE);
                buffer.setLength(0);
                Strings.appendToken(buffer, date.group(2).trim());
            } else if (recordDate != null) {
                Strings.appendToken(buffer, line);
            } else {
                continue; // header/footer noise outside any record
            }

            if (TRAILING_VALUE.matcher(buffer).find()) {
                finalize(recordDate, buffer.toString(), lines);
                recordDate = null;
            }
        }

        return new MonetaryDocument.Statement(Issuer.BTG, List.copyOf(lines));
    }

    private static void finalize(LocalDate date, String record, List<MonetaryDocumentEntry> out) {
        val value = TRAILING_VALUE.matcher(record);
        if (!value.find()) {
            return;
        }
        val description = record.substring(0, value.start()).replaceAll("\\s+", " ").trim();
        if (isDropped(description)) {
            return;
        }
        BigDecimal amount = Amounts.brl(value.group(2));
        if (value.group(1) != null) {
            amount = amount.negate();
        }
        out.add(new MonetaryDocumentEntry(date, description, amount));
    }

    private static boolean isDropped(String description) {
        return description.isEmpty()
                || description.contains("Saldo Diário")
                || description.contains("Saldo final")
                || description.contains("Pagamento de fatura do cartão");
    }

}

package br.cdb.feature.f007._2_infrastructure.provider.santander;

import br.cdb.feature.f007._0_domain.Amounts;
import br.cdb.feature.f007._0_domain.MonetaryDocument;
import br.cdb.feature.f007._0_domain.MonetaryDocumentEntry;
import br.cdb.feature.f007._0_domain.StatementParser;
import br.cdb.feature.f007._2_infrastructure.provider.DocumentText;
import br.commons.Logger;
import br.commons.chrono.Dates;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Santander checking-account statement parser ("Extrato Consolidado Inteligente").
 *
 * <p>Only the {@code Movimentação} table is read; it opens with the column header {@code "Data
 * Descrição Nº Documento Movimento (R$) Saldo (R$)"} and closes at the {@code SALDO EM} value line
 * (the {@code Saldos por Período} table that follows is a hard stop). Everything outside that window —
 * the cover, the {@code Resumo}, and the later {@code Débito Automático}/{@code Transferências} tables
 * — is ignored, which matters because those tables carry their own date+amount rows that would
 * otherwise be mistaken for movements.
 *
 * <p>PDFBox emits one movement as: a posting line {@code "dd/mm  TIPO"} (two+ spaces separate the
 * Data column from the description) followed by continuation lines (counterparty, merchant + its
 * year-less origin date, the {@code dd/mm hh:mm} timestamp, the {@code PERIODO} of a fee) and closed
 * by the value tail {@code "<doc> <movimento>[-] [<saldo>[-]]"} — where the doc is {@code "-"} or a
 * document number, a trailing {@code -} marks a debit (credits print none), and the optional second
 * amount is the running value, which is dropped. The value tail may share the line with the
 * description (e.g. {@code "REMUNERACAO APLICACAO AUTOMATICA - 0,01 1.834,81-"}). The year is absent
 * from each date and taken from the {@code mês/ano} header (movements are always within that month).
 *
 * <p>A continuation line that begins with a date is told apart from a real posting line by spacing: a
 * posting line has two+ spaces after {@code dd/mm} (the column gap), a merchant/timestamp line has a
 * single one.
 *
 * <p>Dropped: value lines and the aggregate credit-card invoice settlement ({@code PAGAMENTO
 * CARTAO...}, and a {@code Fatura Cartão} paid by boleto), because the credit-card invoice import
 * already posts those charges individually onto the linked account.
 */
@NullMarked
public class SantanderStatementParser implements StatementParser {

    private static final Pattern MONTH_TOKEN = Pattern.compile(
            "(janeiro|fevereiro|março|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)/(\\d{4})",
            Pattern.CASE_INSENSITIVE);

    /** A posting line: {@code dd/mm} followed by two+ spaces (the Data→Descrição column gap). */
    private static final Pattern DATE_PREFIX = Pattern.compile("^(\\d{2})/(\\d{2})\\s{2,}(\\S.*)$");
    /** The value tail at the end of a record: {@code <doc> <movimento>[-] [<saldo>[-]]}. */
    private static final Pattern TRAILING_VALUE = Pattern.compile(
            "\\s(?:-|\\d{3,})\\s+(\\d[\\d.]*,\\d{2})(-?)(?:\\s+\\d[\\d.]*,\\d{2}-?)?\\s*$");

    private static final String STATEMENT_MARKER = "EXTRATO CONSOLIDADO INTELIGENTE";

    @Override
    public boolean parseable(String raw) {
        return looksLikeStatement(new DocumentText(raw));
    }

    /** Santander stamps "Extrato Consolidado Inteligente"; its extrato carries no CNPJ. */
    public static boolean looksLikeStatement(DocumentText text) {
        return text.has(STATEMENT_MARKER);
    }

    @Override
    public MonetaryDocument parse(String text) {
        Logger.debug("Parsing Statement");
        val year = referenceYear(text);
        val refMonth = referenceMonth(text);
        val lines = new ArrayList<MonetaryDocumentEntry>();

        boolean inSection = false;
        @Nullable LocalDate recordDate = null;
        val buffer = new StringBuilder();

        for (val raw : text.split("\\R", -1)) {
            val line = raw.strip();

            if (!inSection) {
                inSection = line.contains("Movimento (R"); // the Movimentação column header opens the table
                continue;
            }
            if (isStop(line, recordDate)) {
                break;
            }
            if (skip(line)) {
                continue;
            }

            val date = DATE_PREFIX.matcher(line);
            if (date.find()) {
                recordDate = dateFor(year, refMonth, Integer.parseInt(date.group(2)), Integer.parseInt(date.group(1)));
                buffer.setLength(0);
                Strings.appendToken(buffer, date.group(3).strip());
                finalizeIfComplete(recordDate, buffer, lines);
                continue;
            }

            if (recordDate == null) {
                continue; // noise before the first movement
            }
            Strings.appendToken(buffer, line);
            finalizeIfComplete(recordDate, buffer, lines);
        }

        return new MonetaryDocument.Statement("Santander", List.copyOf(lines));
    }

    /** Fim da janela de movimentos: a tabela {@code Saldos por Período} ou o saldo de fechamento. */
    private static boolean isStop(String line, @Nullable LocalDate recordDate) {
        if (line.startsWith("Saldos por Per")) {
            return true; // next section — its rows must not be parsed as movements
        }
        return line.contains("SALDO EM") && recordDate != null; // closing value ends the window
    }

    /** Linhas ignoradas dentro da tabela: header repetido, saldo de abertura, vazias e ruído de página. */
    private static boolean skip(String line) {
        return line.contains("Movimento (R")  // header repeats on every page
                || line.contains("SALDO EM")   // opening value, before the first movement
                || line.isEmpty() || isNoise(line);
    }

    private static void finalizeIfComplete(LocalDate date, StringBuilder buffer, List<MonetaryDocumentEntry> out) {
        val value = TRAILING_VALUE.matcher(buffer);
        if (!value.find()) {
            return;
        }
        val description = buffer.substring(0, value.start()).replaceAll("\\s+", " ").strip();
        BigDecimal amount = Amounts.brl(value.group(1));
        if ("-".equals(value.group(2))) {
            amount = amount.negate();
        }
        buffer.setLength(0);
        if (description.isEmpty() || isDropped(description)) {
            return;
        }
        out.add(new MonetaryDocumentEntry(date, description, amount));
    }

    private static boolean isDropped(String description) {
        val upper = Strings.upper(description);
        return upper.contains("PAGAMENTO CARTAO") || upper.contains("PAGAMENTO CARTÃO")
                || upper.contains("FATURA CARTAO") || upper.contains("FATURA CARTÃO");
    }

    private static boolean isNoise(String line) {
        return line.startsWith("EXTRATO CONSOLIDADO")
                || line.startsWith("Extrato_")
                || line.startsWith("BALP")
                || line.startsWith("Pagina")
                || MONTH_TOKEN.matcher(line).matches();
    }

    /** Calendar-month statements only spill across a year at the Dec/Jan boundary; guard for it. */
    private static LocalDate dateFor(int year, int refMonth, int month, int day) {
        val resolved = month > refMonth ? year - 1 : (month < refMonth ? year + 1 : year);
        return LocalDate.of(resolved, month, day);
    }

    private static int referenceYear(String text) {
        val m = MONTH_TOKEN.matcher(text);
        return m.find() ? Integer.parseInt(m.group(2)) : Year.now(ZoneId.systemDefault()).getValue();
    }

    private static int referenceMonth(String text) {
        val m = MONTH_TOKEN.matcher(text);
        return m.find() ? Dates.MONTHS_PTBR.getOrDefault(Strings.upper(m.group(1)), 1) : 1;
    }

}

package br.cdb.feature.f007._2_infrastructure.provider.sams_club;

import br.cdb.feature.f007._0_domain.model.ChargeKind;
import br.cdb.feature.f007._0_domain.model.MonetaryDocument;
import br.cdb.feature.f007._0_domain.model.MonetaryDocumentEntry;
import br.cdb.feature.f007._1_application.StatementParser;
import br.cdb.feature.f007._2_infrastructure.provider.Amounts;
import br.cdb.feature.f007._2_infrastructure.provider.BankStatements;
import br.cdb.feature.f007._2_infrastructure.provider.DocumentText;
import br.commons.Logger;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.DateTimeException;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Sam's Club (Banco CSF S.A.) invoice parser.
 *
 * <p>The document opens with the boleto/ficha de compensação — the same masked PAN and the same
 * {@code dd/MM/yyyy} due date repeat several times there — and only then prints the charges, under the
 * {@code LANÇAMENTOS NO BRASIL} header, as {@code DD/MM description value} (year-less date first, BRL
 * value last) — e.g. {@code 01/07 SAMS 4953 ACS AGUAS CLARA 740,79}. Charge-vs-skip is section driven:
 * everything before that header (boleto, rates table, payment alternatives) and everything from the
 * closing {@code TOTAL DA FATURA} / {@code RESUMO DA FATURA} on is dropped, which also drops the
 * {@code SALDO FATURA ANTERIOR} carry-over line inside the section (it carries no date, so it never
 * matches anyway).
 *
 * <p>Cards are tagged the way the invoice itself groups them: inside the section, a bare
 * {@code holder + masked PAN} line ({@code NNNNNN******NNNN}) switches the following lines to that
 * card. The header's own {@code CARTÃO: NNNNNN******NNNN} is the fallback for a single-holder invoice
 * that lists charges without repeating the holder line.
 *
 * <p>The period is the {@code VENCIMENTO} due date printed under the invoice total (the boleto's own
 * mixed-case "Data de Vencimento" is deliberately not matched — same date, but the uppercase headline
 * is the invoice's own, not the bank slip's).
 *
 * <p>Two known omissions, both because the reference document does not exercise them: the
 * {@code ENCARGOS FINANCEIROS} block (empty on a fully-paid invoice, line format unverified) is not
 * read, and a negative amount — payment received or estorno, indistinguishable from each other here —
 * is dropped rather than emitted as a credit.
 */
@NullMarked
public class SamsClubInvoiceParser implements StatementParser {

    /**
     * The invoice's own headline ("FATURA MENSAL CARTÃO SAM'S CLUB GOLD MASTERCARD"), matched with the
     * apostrophe left loose (PDF text layers print it as {@code '} or {@code ’}). The brand is required
     * in this headline position, never on its own: "SAMS CLUB" also shows up as a merchant name on
     * other issuers' invoices, and two parsers claiming the same document fails the import as
     * {@code UnknownIssuer}.
     */
    private static final Pattern BRAND = Pattern.compile("CART[ÃA]O SAM.{0,2}S CLUB");

    private static final Pattern TITULAR_CARD = Pattern.compile("CART[ÃA]O:\\s*\\d{6}\\*+(\\d{4})");
    private static final Pattern CARD_HEADER = Pattern.compile("^(?:.*\\s)?\\d{6}\\*+(\\d{4})$");
    private static final Pattern TXN = Pattern.compile(
            "^\\s*(\\d{2})/(\\d{2})\\s+(.+?)(?:\\s+(?:PARC(?:ELA)?\\s+)?(\\d{2})/(\\d{2}))?\\s+(?:R\\$\\s*)?(-?)([\\d.]+,\\d{2})(-?)\\s*$");
    private static final Pattern VENCIMENTO = Pattern.compile("VENCIMENTO(?:[^\\n\\r]*\\R)?[^\\n\\r]*?(\\d{2})/(\\d{2})/(\\d{4})");
    private static final Pattern SECTION_START = Pattern.compile("LAN[ÇC]AMENTOS NO BRASIL");

    /** Prefixo impresso na descrição → natureza do lançamento; o que não casa é compra. */
    private static final Map<String, ChargeKind> KIND_BY_PREFIX = Map.of(
            "ANUIDADE", ChargeKind.FEE,
            "TARIFA", ChargeKind.FEE,
            "SEGURO", ChargeKind.FEE,
            "IOF", ChargeKind.IOF,
            "JUROS", ChargeKind.INTEREST,
            "ENCARGOS", ChargeKind.INTEREST,
            "MULTA", ChargeKind.INTEREST,
            "MORA", ChargeKind.INTEREST);

    /** Headers that close the BRL charge list — including the foreign block, whose US$ lines this slice does not read. */
    private static final Pattern SECTION_END = Pattern.compile(
            "^(TOTAL DA FATURA|RESUMO DA FATURA|SALDOS FUTUROS|LIMITES EM|LAN[ÇC]AMENTOS NO EXTERIOR|RESUMO DAS DESPESAS NO EXTERIOR)\\b.*");

    @Override
    public boolean parseable(String raw) {
        val text = new DocumentText(raw);
        return !BankStatements.isAny(text) && BRAND.matcher(Strings.upper(raw)).find();
    }

    @Override
    public MonetaryDocument parse(String text) {
        Logger.debug("Parsing Invoice");
        val lines = new ArrayList<MonetaryDocumentEntry>();
        @Nullable String last4 = titularLast4(text);
        boolean keep = false;

        for (val line : text.split("\\R", -1)) {
            val trimmed = line.trim();
            val section = sectionKeep(trimmed);
            if (section != null) {
                keep = section;
                continue;
            }
            if (!keep) {
                continue;
            }

            val card = CARD_HEADER.matcher(trimmed);
            if (card.matches()) {
                last4 = card.group(1);
                continue;
            }

            val txn = txnLine(trimmed, last4);
            if (txn != null) {
                lines.add(txn);
            }
        }

        // Per-card printed totals are not captured: the invoice prints a single "TOTAL DA FATURA" for
        // the whole document (previous balance and encargos included), which is not the per-card
        // checksum reconciliation compares against.
        return new MonetaryDocument.Invoice("Sam's Club", period(text), List.copyOf(lines), Map.of());
    }

    /**
     * The invoice's own printed period, taken from the "VENCIMENTO" due-date (same month as the
     * fatura); {@code null} if absent, in which case the caller anchors on the upload date instead.
     */
    private static @Nullable YearMonth period(String text) {
        val m = VENCIMENTO.matcher(text);
        if (!m.find()) {
            Logger.warn("Sam's Club invoice period (VENCIMENTO) not found, caller will anchor on the upload date");
            return null;
        }
        return YearMonth.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)));
    }

    /** O cartão do titular impresso no cabeçalho — tag padrão das linhas até um portador aparecer. */
    private static @Nullable String titularLast4(String text) {
        val m = TITULAR_CARD.matcher(Strings.upper(text));
        return m.find() ? m.group(1) : null;
    }

    /** {@code true}/{@code false} se a linha alterna a seção de interesse; {@code null} caso contrário. */
    private static @Nullable Boolean sectionKeep(String trimmed) {
        val upper = Strings.upper(trimmed);
        if (SECTION_START.matcher(upper).find()) {
            return Boolean.TRUE;
        }
        if (SECTION_END.matcher(upper).matches()) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static @Nullable MonetaryDocumentEntry txnLine(String line, @Nullable String last4) {
        val m = TXN.matcher(line);
        if (!m.matches()) {
            return null;
        }
        // The minus may be printed on either side of the value; a credit (payment / estorno) is dropped.
        if (!m.group(6).isEmpty() || !m.group(8).isEmpty()) {
            return null;
        }
        val date = monthDay(m.group(1), m.group(2));
        if (date == null) {
            return null;
        }
        val number = m.group(4) == null ? 1 : Integer.parseInt(m.group(4));
        val total = m.group(5) == null ? 1 : Integer.parseInt(m.group(5));
        val description = m.group(3).trim();
        return MonetaryDocumentEntry.charge(last4, date, description, Amounts.brl(m.group(7)), number, total, classify(description));
    }

    /** {@code DD/MM} impresso; {@code null} quando não é uma data real (linha de texto que só se parece com lançamento). */
    private static @Nullable MonthDay monthDay(String day, String month) {
        try {
            return MonthDay.of(Integer.parseInt(month), Integer.parseInt(day));
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static ChargeKind classify(String description) {
        val upper = Strings.upper(description);
        for (val prefix : KIND_BY_PREFIX.entrySet()) {
            if (upper.startsWith(prefix.getKey())) {
                return prefix.getValue();
            }
        }
        return ChargeKind.PURCHASE;
    }
}

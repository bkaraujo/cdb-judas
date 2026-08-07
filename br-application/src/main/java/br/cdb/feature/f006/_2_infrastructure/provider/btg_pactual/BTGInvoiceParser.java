package br.cdb.feature.f006._2_infrastructure.provider.btg_pactual;

import br.cdb.feature.f006._0_domain.*;
import br.cdb.feature.f006._2_infrastructure.provider.BankStatements;
import br.cdb.feature.f006._2_infrastructure.provider.DocumentText;
import br.commons.Logger;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.commons.chrono.Dates.MONTHS_PTBR;

/**
 * BTG Pactual statement parser.
 *
 * <p>BTG prints the amount first, then the description, an optional {@code (n/N)} installment marker
 * and a year-less {@code DD Mon} date — e.g. {@code R$ 72,99Amazonmktplc Megabytem (9/10)15 Jul}.
 * Charge-vs-skip is driven by section + sub-header, not per-line wording: each physical/virtual card
 * block (headed by {@code … Final NNNN Total do cartão …}, which also carries the card's own printed
 * checksum — see {@link #captureCardTotal}) keeps the lines under its {@code Total de compras e
 * despesas} sub-header as {@code EXPENSE} and the lines under {@code Total de créditos recebidos} as
 * {@code INCOME}, dropping everything else — payments, fatura financing ({@code Parcelamento de
 * fatura}), the {@code Mensalidade}/{@code Resumo da Fatura Atual}/{@code Taxas e encargos} blocks.
 *
 * <p>A block is closed only by an explicit marker — the next {@code CARD_HEADER} or one of the known
 * section headers above — never by a lone amount. BTG re-prints page furniture (footer, the invoice's
 * own headline) whenever a card's transaction list crosses a page break, and a lone amount line is
 * indistinguishable from that furniture; worse, a card's running subtotal can coincidentally equal its
 * final total when it has no credits, so it can't be used as a "block closed" sentinel either — see
 * {@code fatura-btg-fev25.txt}#0020, whose 86 lines split across two pages this way.
 *
 * <p>International (US$) lines are not emitted: the BRL amount is not on the line, so this slice
 * keeps only the R$ charges.
 */
@NullMarked
public class BTGInvoiceParser implements StatementParser {

    private static final Pattern CARD_HEADER = Pattern.compile(
            "Final (\\d{4}) Total do cart[ãa]o(?::\\s*(-)?R\\$\\s*([\\d.]+,\\d{2}))?");
    private static final Pattern TXN = Pattern.compile("^\\s*-?\\s*(R\\$|US\\$)\\s*([\\d.]+,\\d{2})(.+?)(?:\\((\\d+)/(\\d+)\\))?(\\d{2}) ([A-Za-z]{3})$");
    private static final Pattern FATURA_PERIOD = Pattern.compile("(?i)fatura de (\\p{L}+) de (\\d{4})");

    private static final String KEEP_HEADER = "Total de compras e despesas";
    private static final String CREDIT_HEADER = "Total de créditos recebidos";

    /** Known section headers that close whatever block is open without opening a new one. */
    private static final Set<String> SECTION_ENDERS = Set.of(
            "Pagamentos feitos pelo cliente",
            "Parcelamento de fatura",
            "Mensalidade",
            "Resumo da Fatura Atual",
            "Taxas e encargos"
    );

    /** BTG's card-issuer CNPJ (…0001-45), distinct from the checking-account CNPJ (…0002-26) on the extrato. */
    private static final String ISSUER_CNPJ = "30306294000145";

    private enum Section { OUT, CREDIT, PURCHASE }

    @Override
    public boolean parseable(String raw) {
        val text = new DocumentText(raw);
        return !BankStatements.isAny(text)
                && (text.hasCnpj(ISSUER_CNPJ) || text.nameOnly("BTG PACTUAL", "SANTANDER"));
    }

    @Override
    public MonetaryDocument parse(String text) {
        Logger.debug("Parsing Invoice");
        val lines = new ArrayList<MonetaryDocumentEntry>();
        val printedTotals = new LinkedHashMap<String, BigDecimal>();
        String last4 = null;
        var section = Section.OUT;

        for (val line : text.split("\\R", -1)) {
            val card = CARD_HEADER.matcher(line);
            if (card.find()) {
                last4 = card.group(1);
                section = Section.OUT;
                captureCardTotal(card, last4, printedTotals);
                continue;
            }

            val marker = sectionMarker(line);
            if (marker != null) {
                section = marker;
                continue;
            }

            if (section == Section.OUT || last4 == null) {
                continue;
            }

            val txn = txnLine(line, last4, section);
            if (txn != null) {
                lines.add(txn);
            }
        }

        return new MonetaryDocument.Invoice("BTG Pactual", period(text), List.copyOf(lines), Map.copyOf(printedTotals));
    }

    /**
     * The invoice's own printed period ("fatura de Fevereiro de 2025"); {@code null} if absent, in
     * which case the caller anchors on the upload date instead.
     */
    private static @Nullable YearMonth period(String text) {
        val m = FATURA_PERIOD.matcher(text);
        if (!m.find()) {
            Logger.warn("BTG invoice period header not found, caller will anchor on the upload date");
            return null;
        }
        val monthValue = month(m.group(1));
        return monthValue == 0 ? null : YearMonth.of(Integer.parseInt(m.group(2)), monthValue);
    }

    /** The card's own printed checksum ("Total do cartão: R$ X"); logs and skips (never fails) if absent. */
    private static void captureCardTotal(Matcher card, String last4, Map<String, BigDecimal> out) {
        if (card.group(3) == null) {
            Logger.warn("BTG card total not captured for final %s, reconciliation skipped for this card", last4);
            return;
        }
        var value = Amounts.brl(card.group(3));
        if (card.group(2) != null) {
            value = value.negate();
        }
        out.put(last4, value);
    }

    /** {@code Section} the line switches to, if it's a section marker; {@code null} otherwise. */
    private static @Nullable Section sectionMarker(String line) {
        val trimmed = line.trim();
        if (trimmed.equals(KEEP_HEADER)) {
            return Section.PURCHASE;
        }
        if (trimmed.equals(CREDIT_HEADER)) {
            return Section.CREDIT;
        }
        if (SECTION_ENDERS.contains(trimmed)) {
            return Section.OUT;
        }
        return null;
    }

    private static @Nullable MonetaryDocumentEntry txnLine(String line, String last4, Section section) {
        val m = TXN.matcher(line);
        if (!m.matches() || m.group(1).equals("US$")) { return null; }

        val month = month(m.group(7));
        if (month == 0) { return null; }

        val date = MonthDay.of(month, Integer.parseInt(m.group(6)));
        val description = m.group(3).trim();
        val amount = Amounts.brl(m.group(2));

        if (section == Section.CREDIT) {
            // Credits never carry a real installment schedule (their "n/N"-looking text, when
            // present, is part of the description — e.g. "Desconto Na Parcela 12 De 12").
            return MonetaryDocumentEntry.credit(last4, date, description, amount, ChargeKind.PURCHASE);
        }

        val number = m.group(4) == null ? 1 : Integer.parseInt(m.group(4));
        val total = m.group(5) == null ? 1 : Integer.parseInt(m.group(5));
        return MonetaryDocumentEntry.charge(last4, date, description, amount, number, total, ChargeKind.PURCHASE);
    }

    private static int month(String abbr) {
        val upper = Strings.upper(abbr);
        for (val month : MONTHS_PTBR.keySet()) {
            if (month.startsWith(upper)) {
                return MONTHS_PTBR.get(month);
            }
        }

        return 0;
    }
}

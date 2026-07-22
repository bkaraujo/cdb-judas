package br.cdb.context.monetary;

import br.cdb.feature.f006._0_domain.ChargeKind;
import br.cdb.feature.f006._0_domain.MonetaryDocument;
import br.cdb.feature.f006._0_domain.MonetaryDocumentEntry;
import br.cdb.feature.f006._2_infrastructure.provider.BTGInvoiceParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.MonthDay;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** BTG parsing against the anonymized fixtures, asserted through the parser's public output. */
class BTGInvoiceParserTest {

    private final BTGInvoiceParser parser = new BTGInvoiceParser();

    @Test
    void readsLast4sOfCardsThatHaveKeptCharges() throws IOException {
        List<MonetaryDocumentEntry> st = parsed("fatura-btg-abril.txt");
        // Charges live under the sub-cards (físico/virtual), not the account-level "final 5115".
        assertEquals(List.of("0020", "9822"), last4s(st));
    }

    @Test
    void keepsOnlyComprasLinesUnderEachCard() throws IOException {
        List<MonetaryDocumentEntry> st = parsed("fatura-btg-abril.txt");
        assertEquals(24, st.size());
        assertTrue(st.stream().allMatch(l -> l.kind() == ChargeKind.PURCHASE));
        assertTrue(st.stream().noneMatch(l -> l.amount().signum() < 0));
    }

    @Test
    void parsesInstallmentAndOriginalDateAndAmount() throws IOException {
        List<MonetaryDocumentEntry> st = parsed("fatura-btg-abril.txt");
        assertTrue(st.stream().anyMatch(l ->
                l.description().equals("Amazonmktplc Megabytem")
                        && Integer.valueOf(9).equals(l.installmentNumber())
                        && Integer.valueOf(10).equals(l.installmentTotal())
                        && MonthDay.from(l.date()).equals(MonthDay.of(7, 15))
                        && l.amount().compareTo(new BigDecimal("72.99")) == 0
                        && l.last4().equals("0020")));
    }

    @Test
    void aVistaLineHasNoInstallment() throws IOException {
        List<MonetaryDocumentEntry> st = parsed("fatura-btg-abril.txt");
        assertTrue(st.stream().anyMatch(l ->
                l.description().equals("Microsoft")
                        && l.installmentNumber() == 1
                        && l.installmentTotal() == 1
                        && MonthDay.from(l.date()).equals(MonthDay.of(3, 9))
                        && l.amount().compareTo(new BigDecimal("60.00")) == 0
                        && l.last4().equals("9822")));
    }

    @Test
    void dropsPaymentsFaturaFinancingCreditsAndSummary() throws IOException {
        List<MonetaryDocumentEntry> st = parsed("fatura-btg-abril.txt");
        assertFalse(st.stream().anyMatch(l -> l.description().contains("Pagamento de fatura")));
        assertFalse(st.stream().anyMatch(l -> l.description().startsWith("Parcelamento da Fatura")));
        assertFalse(st.stream().anyMatch(l -> l.description().contains("Desconto")));
        assertFalse(st.stream().anyMatch(l -> l.description().contains("Cancelamento")));
    }

    @Test
    void crossMonthInstallmentAdvancesKeepingDateAndAmount() throws IOException {
        List<MonetaryDocumentEntry> st = parsed("fatura-btg-maio.txt");
        assertEquals(List.of("0020", "9822"), last4s(st));
        assertEquals(17, st.size());
        // Same purchase as April, now billed 10/10, original date and amount unchanged.
        assertTrue(st.stream().anyMatch(l ->
                l.description().equals("Amazonmktplc Megabytem")
                        && Integer.valueOf(10).equals(l.installmentNumber())
                        && Integer.valueOf(10).equals(l.installmentTotal())
                        && MonthDay.from(l.date()).equals(MonthDay.of(7, 15))
                        && l.amount().compareTo(new BigDecimal("72.99")) == 0));
    }

    @Test
    void dropsInternationalUsdLineAndCreditCardBlock() throws IOException {
        List<MonetaryDocumentEntry> st = parsed("fatura-btg-maio.txt");
        // US$ line: BRL not on the line, not emitted in this slice.
        assertFalse(st.stream().anyMatch(l -> l.description().contains("Amazon Web Services")));
        // The "Total de créditos recebidos" card (Final 5115) is entirely dropped.
        assertFalse(st.stream().anyMatch(l -> l.last4().equals("5115")));
    }

    private static List<String> last4s(List<MonetaryDocumentEntry> lines) {
        return lines.stream().map(MonetaryDocumentEntry::last4).distinct().toList();
    }

    private List<MonetaryDocumentEntry> parsed(String name) throws IOException {
        return ((MonetaryDocument.Invoice) parser.parse(fixture(name))).statement();
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = BTGInvoiceParserTest.class.getResourceAsStream("/faturas/" + name)) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

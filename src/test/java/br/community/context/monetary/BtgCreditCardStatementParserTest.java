package br.community.context.monetary;

import br.community.feature.user.accounts.statementimport.BtgCreditCardStatementParser;
import br.community.feature.user.accounts.statementimport.ChargeKind;
import br.community.feature.user.accounts.statementimport.ParsedStatement;
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
class BtgCreditCardStatementParserTest {

    private final BtgCreditCardStatementParser parser = new BtgCreditCardStatementParser();

    @Test
    void readsLast4sOfCardsThatHaveKeptCharges() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-btg-abril.txt"));
        // Charges live under the sub-cards (físico/virtual), not the account-level "final 5115".
        assertEquals(List.of("0020", "9822"), st.last4s());
    }

    @Test
    void keepsOnlyComprasLinesUnderEachCard() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-btg-abril.txt"));
        assertEquals(24, st.lines().size());
        assertTrue(st.lines().stream().allMatch(l -> l.kind() == ChargeKind.PURCHASE));
        assertTrue(st.lines().stream().noneMatch(l -> l.amount().signum() < 0));
    }

    @Test
    void parsesInstallmentAndOriginalDateAndAmount() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-btg-abril.txt"));
        assertTrue(st.lines().stream().anyMatch(l ->
                l.description().equals("Amazonmktplc Megabytem")
                        && Integer.valueOf(9).equals(l.installmentNumber())
                        && Integer.valueOf(10).equals(l.installmentTotal())
                        && l.purchaseDate().equals(MonthDay.of(7, 15))
                        && l.amount().compareTo(new BigDecimal("72.99")) == 0
                        && l.last4().equals("0020")));
    }

    @Test
    void aVistaLineHasNoInstallment() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-btg-abril.txt"));
        assertTrue(st.lines().stream().anyMatch(l ->
                l.description().equals("Microsoft")
                        && l.installmentNumber() == null
                        && l.installmentTotal() == null
                        && l.purchaseDate().equals(MonthDay.of(3, 9))
                        && l.amount().compareTo(new BigDecimal("60.00")) == 0
                        && l.last4().equals("9822")));
    }

    @Test
    void dropsPaymentsFaturaFinancingCreditsAndSummary() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-btg-abril.txt"));
        assertFalse(st.lines().stream().anyMatch(l -> l.description().contains("Pagamento de fatura")));
        assertFalse(st.lines().stream().anyMatch(l -> l.description().startsWith("Parcelamento da Fatura")));
        assertFalse(st.lines().stream().anyMatch(l -> l.description().contains("Desconto")));
        assertFalse(st.lines().stream().anyMatch(l -> l.description().contains("Cancelamento")));
    }

    @Test
    void crossMonthInstallmentAdvancesKeepingDateAndAmount() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-btg-maio.txt"));
        assertEquals(List.of("0020", "9822"), st.last4s());
        assertEquals(17, st.lines().size());
        // Same purchase as April, now billed 10/10, original date and amount unchanged.
        assertTrue(st.lines().stream().anyMatch(l ->
                l.description().equals("Amazonmktplc Megabytem")
                        && Integer.valueOf(10).equals(l.installmentNumber())
                        && Integer.valueOf(10).equals(l.installmentTotal())
                        && l.purchaseDate().equals(MonthDay.of(7, 15))
                        && l.amount().compareTo(new BigDecimal("72.99")) == 0));
    }

    @Test
    void dropsInternationalUsdLineAndCreditCardBlock() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-btg-maio.txt"));
        // US$ line: BRL not on the line, not emitted in this slice.
        assertFalse(st.lines().stream().anyMatch(l -> l.description().contains("Amazon Web Services")));
        // The "Total de créditos recebidos" card (Final 5115) is entirely dropped.
        assertFalse(st.lines().stream().anyMatch(l -> l.last4().equals("5115")));
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = BtgCreditCardStatementParserTest.class.getResourceAsStream("/faturas/" + name)) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

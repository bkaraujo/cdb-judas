package br.community.context.monetary;

import br.community.context.monetary._0_domain.model.ChargeKind;
import br.community.context.monetary._0_domain.model.ParsedStatement;
import br.community.context.monetary._1_application.service.SantanderCreditCardStatementParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.MonthDay;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** Santander parsing against the anonymized fixtures, asserted through the parser's public output. */
class SantanderCreditCardStatementParserTest {

    private final SantanderCreditCardStatementParser parser = new SantanderCreditCardStatementParser();

    @Test
    void readsEachCardLast4FromMaskedPan() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-santander-abril.txt"));
        assertEquals(List.of("1258", "2884"), st.last4s());
    }

    @Test
    void parsesParcelamentoLineWithInstallmentDateAndAmount() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-santander-abril.txt"));
        assertTrue(st.lines().stream().anyMatch(l ->
                l.description().equals("DECATHLON")
                        && Integer.valueOf(8).equals(l.installmentNumber())
                        && Integer.valueOf(10).equals(l.installmentTotal())
                        && l.purchaseDate().equals(MonthDay.of(8, 3))
                        && l.amount().compareTo(new BigDecimal("111.98")) == 0
                        && l.last4().equals("1258")
                        && l.kind() == ChargeKind.PURCHASE));
    }

    @Test
    void parsesAVistaDespesaWithoutInstallment() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-santander-abril.txt"));
        assertTrue(st.lines().stream().anyMatch(l ->
                l.description().equals("NETFLIX COM")
                        && l.installmentNumber() == null
                        && l.purchaseDate().equals(MonthDay.of(3, 6))
                        && l.amount().compareTo(new BigDecimal("72.80")) == 0
                        && l.last4().equals("1258")));
    }

    @Test
    void keepsAnnuityAsFeeAndForeignIofAnchoredToItsPurchaseDate() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-santander-abril.txt"));
        // ANUIDADE DIFERENCIADA (R$ 0,00) kept as a FEE charge.
        assertTrue(st.lines().stream().anyMatch(l ->
                l.kind() == ChargeKind.FEE
                        && l.description().startsWith("ANUIDADE")
                        && l.amount().compareTo(new BigDecimal("0.00")) == 0));
        // Dateless foreign IOF kept as IOF, anchored to the preceding foreign purchase (VIKI COM, 20/03).
        assertTrue(st.lines().stream().anyMatch(l ->
                l.kind() == ChargeKind.IOF
                        && l.description().contains("IOF DESPESA")
                        && l.amount().compareTo(new BigDecimal("1.41")) == 0
                        && l.purchaseDate().equals(MonthDay.of(3, 20))));
    }

    @Test
    void dropsPaymentsCreditsRefundsAndSummary() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-santander-abril.txt"));
        assertFalse(st.lines().stream().anyMatch(l -> l.description().contains("PAGAMENTO DE FATURA")));
        assertFalse(st.lines().stream().anyMatch(l -> l.description().contains("Saldo")));
        assertFalse(st.lines().stream().anyMatch(l -> l.amount().signum() < 0));
    }

    @Test
    void parsesAllCardsAcrossTwoCardholders() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-santander-maio.txt"));
        assertEquals(List.of("4628", "1258", "2884", "1922", "8376"), st.last4s());
    }

    @Test
    void crossMonthInstallmentAdvancesKeepingOriginalDate() throws IOException {
        ParsedStatement st = parser.parse(fixture("fatura-santander-maio.txt"));
        // DECATHLON now 09/10; original purchase date unchanged from April (03/08).
        assertTrue(st.lines().stream().anyMatch(l ->
                l.description().equals("DECATHLON")
                        && Integer.valueOf(9).equals(l.installmentNumber())
                        && Integer.valueOf(10).equals(l.installmentTotal())
                        && l.purchaseDate().equals(MonthDay.of(8, 3))
                        && l.amount().compareTo(new BigDecimal("111.98")) == 0));
        // Santander varies the per-installment cents: 037 - DF TAGUATINGA CT bills 231,79 as 3/3.
        assertTrue(st.lines().stream().anyMatch(l ->
                l.description().equals("037 - DF TAGUATINGA CT")
                        && Integer.valueOf(3).equals(l.installmentNumber())
                        && l.amount().compareTo(new BigDecimal("231.79")) == 0));
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = SantanderCreditCardStatementParserTest.class.getResourceAsStream("/faturas/" + name)) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

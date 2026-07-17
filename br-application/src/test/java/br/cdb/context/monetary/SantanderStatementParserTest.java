package br.cdb.context.monetary;

import br.cdb.feature.user.accounts.statement.MonetaryDocument;
import br.cdb.feature.user.accounts.statement.MonetaryDocumentEntry;
import br.cdb.feature.user.accounts.statement.provider.SantanderStatementParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Santander checking-account ("Extrato Consolidado Inteligente") parsing, asserted through the parser's output. */
class SantanderStatementParserTest {

    private final SantanderStatementParser parser = new SantanderStatementParser();

    @Test
    void signsDebitsNegativeAndCreditsPositive() throws IOException {
        List<MonetaryDocumentEntry> lines = movements();
        // Credit (no trailing minus): TED received and a Pix received.
        assertTrue(has(lines, LocalDate.of(2025, 12, 4), "TED RECEBIDA", "14098.27"));
        assertTrue(has(lines, LocalDate.of(2025, 12, 29), "PIX RECEBIDO", "1800.00"));
        // Debit (trailing minus → negative), with thousands separator.
        assertTrue(has(lines, LocalDate.of(2025, 12, 4), "PIX ENVIADO", "-250.00"));
        assertTrue(has(lines, LocalDate.of(2025, 12, 4), "BANCO EXEMPLO BRASIL", "-1242.31"));
    }

    @Test
    void mergesMultiLineDescriptionAndDropsRunningBalance() throws IOException {
        List<MonetaryDocumentEntry> lines = movements();
        // Fee: type line + PERIODO continuation + value line carrying movimento and saldo.
        assertTrue(lines.stream().anyMatch(l ->
                l.date().equals(LocalDate.of(2025, 12, 1))
                        && l.description().contains("JUROS SALDO UTILIZ ATE LIMITE")
                        && l.description().contains("PERIODO: 01/11 A 30/11/25")
                        && l.amount().compareTo(new BigDecimal("-23.93")) == 0));
        // The running value (552,80) on that same value line must never become a movement amount.
        assertTrue(lines.stream().noneMatch(l -> l.amount().abs().compareTo(new BigDecimal("552.80")) == 0));
    }

    @Test
    void keepsMerchantOriginDateAsDescriptionNotANewRecord() throws IOException {
        List<MonetaryDocumentEntry> lines = movements();
        // "02/12  COMPRA CARTAO DEB MC" / "28/11 PADARIA EXEMPLO LTDA" / "330759 23,00-":
        // the merchant line begins with an origin date but is a continuation (single space), so the
        // record stays on 02/12 and the merchant text is merged in.
        assertTrue(lines.stream().anyMatch(l ->
                l.date().equals(LocalDate.of(2025, 12, 2))
                        && l.description().contains("COMPRA CARTAO DEB MC")
                        && l.description().contains("PADARIA EXEMPLO")
                        && l.amount().compareTo(new BigDecimal("-23.00")) == 0));
        // The origin date must not have produced a separate November record.
        assertTrue(lines.stream().noneMatch(l -> l.date().getMonthValue() == 11));
    }

    @Test
    void carriesTheDateAcrossAPageBreak() throws IOException {
        List<MonetaryDocumentEntry> lines = movements();
        // This movement is printed after a page-break header block; its date (04/12) is the last one seen.
        assertTrue(has(lines, LocalDate.of(2025, 12, 4), "CONTA DE AGUA E ESGOTO", "-165.64"));
    }

    @Test
    void parsesValueSharingTheDescriptionLine() throws IOException {
        List<MonetaryDocumentEntry> lines = movements();
        // "REMUNERACAO APLICACAO AUTOMATICA - 0,01 1.834,81-": description + value on one line.
        assertTrue(has(lines, LocalDate.of(2025, 12, 11), "REMUNERACAO APLICACAO AUTOMATICA", "0.01"));
    }

    @Test
    void dropsTheAggregateCreditCardInvoicePayments() throws IOException {
        List<MonetaryDocumentEntry> lines = movements();
        // Santander's own card auto-debit ("PAGAMENTO CARTAO ...") and a card invoice paid by boleto.
        assertTrue(lines.stream().noneMatch(l -> l.description().toUpperCase().contains("PAGAMENTO CARTAO")));
        assertTrue(lines.stream().noneMatch(l -> l.amount().abs().compareTo(new BigDecimal("13366.18")) == 0));
        assertTrue(lines.stream().noneMatch(l -> l.description().toUpperCase().contains("FATURA CARTAO")));
        assertTrue(lines.stream().noneMatch(l -> l.amount().abs().compareTo(new BigDecimal("3076.08")) == 0));
    }

    @Test
    void ignoresEverythingOutsideTheMovimentacaoTable() throws IOException {
        List<MonetaryDocumentEntry> lines = movements();
        // Opening/closing "SALDO EM" value lines are not movements.
        assertTrue(lines.stream().noneMatch(l -> l.description().contains("SALDO EM")));
        // The "Saldos por Período" / "Débito Automático" / "Transferências" tables that follow carry
        // their own date+amount rows and must not leak in: their zero columns and the Provisão-only
        // value never appear, and none of their headers become a description.
        assertTrue(lines.stream().noneMatch(l -> l.amount().signum() == 0));
        assertTrue(lines.stream().noneMatch(l -> l.amount().abs().compareTo(new BigDecimal("134.04")) == 0));
        assertTrue(lines.stream().noneMatch(l ->
                l.description().contains("Disponível") || l.description().contains("Favorecido")
                        || l.description().contains("Realizado") || l.description().contains("Saldos por")));
    }

    @Test
    void parsesEveryMovementInTheMonthExactlyOnce() throws IOException {
        List<MonetaryDocumentEntry> lines = movements();
        assertEquals(23, lines.size());
        assertTrue(lines.stream().allMatch(l -> l.date().getYear() == 2025 && l.date().getMonthValue() == 12));
    }

    private List<MonetaryDocumentEntry> movements() throws IOException {
        return ((MonetaryDocument.Statement) parser.parse(fixture())).statement();
    }

    private static boolean has(List<MonetaryDocumentEntry> lines, LocalDate date, String descPart, String amount) {
        return lines.stream().anyMatch(l ->
                l.date().equals(date)
                        && l.description().contains(descPart)
                        && l.amount().compareTo(new BigDecimal(amount)) == 0);
    }

    private static String fixture() throws IOException {
        try (InputStream in = SantanderStatementParserTest.class.getResourceAsStream("/extratos/extrato-santander-202512.txt")) {
            return new String(Objects.requireNonNull(in, "fixture").readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

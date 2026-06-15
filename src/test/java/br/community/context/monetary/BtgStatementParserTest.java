package br.community.context.monetary;

import br.community.feature.user.accounts.statement.MonetaryDocument;
import br.community.feature.user.accounts.statement.MonetaryDocumentEntry;
import br.community.feature.user.accounts.statement.provider.BTGStatementParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** BTG bank-statement parsing asserted through the parser's public output, against the 2025 fixture. */
class BtgStatementParserTest {

    private final BTGStatementParser parser = new BTGStatementParser();

    @Test
    void dropsBalanceCardPaymentAndHeaderLines() throws IOException {
        List<MonetaryDocumentEntry> st = parsed();
        assertTrue(st.stream().noneMatch(l -> l.description().contains("Saldo Diário")));
        assertTrue(st.stream().noneMatch(l -> l.description().contains("Saldo final")));
        assertTrue(st.stream().noneMatch(l -> l.description().contains("Pagamento de fatura do cartão")));
        // The print-date header line "01/06/2026 ... Lançamentos: R$ 69,64" must not become a movement.
        assertTrue(st.stream().noneMatch(l -> l.date().getYear() == 2026));
        assertTrue(st.stream().noneMatch(l -> l.amount().compareTo(new BigDecimal("69.64")) == 0));
    }

    @Test
    void signsDebitsNegativeAndCreditsPositive() throws IOException {
        List<MonetaryDocumentEntry> st = parsed();
        // Debit: Odontoprev -R$ 161,43 on 02/01/2025.
        assertTrue(st.stream().anyMatch(l ->
                l.date().equals(LocalDate.of(2025, 1, 2))
                        && l.description().contains("Odontoprev")
                        && l.amount().compareTo(new BigDecimal("-161.43")) == 0));
        // Credit: Pix recebido R$ 45,00 on 09/01/2025.
        assertTrue(st.stream().anyMatch(l ->
                l.date().equals(LocalDate.of(2025, 1, 9))
                        && l.description().contains("Vanessa")
                        && l.amount().compareTo(new BigDecimal("45.00")) == 0));
    }

    @Test
    void keepsMultiLineWrappedRecords() throws IOException {
        List<MonetaryDocumentEntry> st = parsed();
        // Category wrap: "Impostos e / Tributos / IOF limite da conta ... -R$ 2,46".
        assertTrue(st.stream().anyMatch(l ->
                l.description().contains("IOF limite da conta")
                        && l.amount().compareTo(new BigDecimal("-2.46")) == 0));
        // Description wrap: value alone on its own line (R$ 2.273,90).
        assertTrue(st.stream().anyMatch(l ->
                l.description().contains("Secr. Da Receita Federal")
                        && l.amount().compareTo(new BigDecimal("2273.90")) == 0));
        // Thousands separator credit.
        assertTrue(st.stream().anyMatch(l ->
                l.amount().compareTo(new BigDecimal("35700.00")) == 0));
    }

    @Test
    void parsesEveryMonthOfTheYear() throws IOException {
        List<MonetaryDocumentEntry> st = parsed();
        List<MonetaryDocumentEntry> lines = st;
        assertTrue(lines.size() > 100, "expected the full-year statement, got " + lines.size());
        for (int month = 1; month <= 12; month++) {
            final int m = month;
            assertTrue(lines.stream().anyMatch(l -> l.date().getMonthValue() == m && l.date().getYear() == 2025),
                    "no movement parsed for month " + m);
        }
    }

    private List<MonetaryDocumentEntry> parsed() throws IOException {
        return ((MonetaryDocument.Statement) parser.parse(fixture())).statement();
    }

    private static String fixture() throws IOException {
        try (InputStream in = BtgStatementParserTest.class.getResourceAsStream("/extratos/extrato-btg-2025.txt")) {
            return new String(Objects.requireNonNull(in, "fixture").readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

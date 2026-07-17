package br.cdb.context.monetary;

import br.cdb.feature.finance.accounts.statement.StatementParser;
import br.cdb.feature.finance.accounts.statement.provider.BTGInvoiceParser;
import br.cdb.feature.finance.accounts.statement.provider.BTGStatementParser;
import br.cdb.feature.finance.accounts.statement.provider.SantanderInvoiceParser;
import br.cdb.feature.finance.accounts.statement.provider.SantanderStatementParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code parseable}-based routing that replaced DocumentTypeDetector + IssuerDetector + the two
 * registries: exactly one parser must claim each real document (issuer + document type folded into the
 * parser), and ambiguous/unrecognized text must be claimed by none — which the use case turns into an
 * UnknownIssuer error. Migrates the disambiguation cases the old detector tests asserted.
 */
class StatementParserSelectionTest {

    private final List<StatementParser> parsers = List.of(
            new BTGStatementParser(),
            new SantanderStatementParser(),
            new BTGInvoiceParser(),
            new SantanderInvoiceParser());

    @Test
    void routesBtgInvoiceFixtures() throws IOException {
        assertSole(BTGInvoiceParser.class, fatura("fatura-btg-abril.txt"));
        assertSole(BTGInvoiceParser.class, fatura("fatura-btg-maio.txt"));
    }

    @Test
    void routesSantanderInvoiceFixtures() throws IOException {
        assertSole(SantanderInvoiceParser.class, fatura("fatura-santander-abril.txt"));
        assertSole(SantanderInvoiceParser.class, fatura("fatura-santander-maio.txt"));
    }

    @Test
    void routesBtgStatementFixture() throws IOException {
        assertSole(BTGStatementParser.class, extrato("extrato-btg-2025.txt"));
    }

    @Test
    void routesSantanderStatementFixture() throws IOException {
        assertSole(SantanderStatementParser.class, extrato("extrato-santander-202512.txt"));
    }

    @Test
    void statementMarkerWinsOverCounterpartyBankName() {
        // A Santander extrato that names "BTG Pactual" as a boleto counterparty is still the Santander statement.
        assertSole(SantanderStatementParser.class,
                "EXTRATO CONSOLIDADO INTELIGENTE\n11/12 Pagamento de boleto Fatura Cartao BTG Pactual -3.076,08");
    }

    @Test
    void cnpjWinsOverCounterpartyBankName() {
        // BTG checking-account statement (account CNPJ ...0002-26) that paid a boleto to "Santander".
        assertSole(BTGStatementParser.class,
                "Extrato BTG Pactual - CNPJ 30.306.294/0002-26\n17/02/2025 13h11 Pagamento de boleto Santander -R$ 1.212,32");
    }

    @Test
    void detectsBtgStatementFromHeaderMarkerAlone() {
        assertSole(BTGStatementParser.class, "Este é o extrato da sua conta corrente BTG Pactual");
    }

    @Test
    void noneClaimUnrecognizedText() {
        assertTrue(capable("Documento sem marcador de banco algum.").isEmpty());
    }

    private void assertSole(Class<? extends StatementParser> expected, String text) {
        final List<StatementParser> capable = capable(text);
        assertEquals(1, capable.size(), () -> "expected exactly one parser, got " + capable);
        assertInstanceOf(expected, capable.getFirst());
    }

    private List<StatementParser> capable(String text) {
        return parsers.stream().filter(parser -> parser.parseable(text)).toList();
    }

    private static String fatura(String name) throws IOException {
        return resource("/faturas/" + name);
    }

    private static String extrato(String name) throws IOException {
        return resource("/extratos/" + name);
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = StatementParserSelectionTest.class.getResourceAsStream(path)) {
            return new String(Objects.requireNonNull(in, path).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

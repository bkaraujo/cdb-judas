package br.community.context.monetary;

import br.community.feature.user.accounts.statementimport.preview.Issuer;
import br.community.feature.user.accounts.statementimport.preview.IssuerDetector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssuerDetectorTest {

    private final IssuerDetector detector = new IssuerDetector();

    @Test
    void detectsBtgFromFixtures() throws IOException {
        assertEquals(Issuer.BTG, detector.detect(fixture("fatura-btg-abril.txt")));
        assertEquals(Issuer.BTG, detector.detect(fixture("fatura-btg-maio.txt")));
    }

    @Test
    void detectsSantanderFromFixtures() throws IOException {
        assertEquals(Issuer.SANTANDER, detector.detect(fixture("fatura-santander-abril.txt")));
        assertEquals(Issuer.SANTANDER, detector.detect(fixture("fatura-santander-maio.txt")));
    }

    @Test
    void detectsBtgFromCheckingAccountStatement() throws IOException {
        try (InputStream in = IssuerDetectorTest.class.getResourceAsStream("/extratos/extrato-btg-2025.txt")) {
            final String text = new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(Issuer.BTG, detector.detect(text));
        }
    }

    @Test
    void cnpjWinsOverCounterpartyBankName() {
        // BTG checking-account statement that paid a boleto to "Santander": the account CNPJ is
        // authoritative, the counterparty name must not make detection ambiguous.
        final String text = "Extrato BTG Pactual - CNPJ 30.306.294/0002-26\n"
                + "17/02/2025 13h11 Pagamento de boleto Santander -R$ 1.212,32";
        assertEquals(Issuer.BTG, detector.detect(text));
    }

    @Test
    void detectsBtgFromStatementHeaderMarkerAlone() {
        assertEquals(Issuer.BTG, detector.detect("Este é o extrato da sua conta corrente BTG Pactual"));
    }

    @Test
    void returnsUnknownForUnrecognizedText() {
        assertEquals(Issuer.UNKNOWN, detector.detect("Documento sem marcador de banco algum."));
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = IssuerDetectorTest.class.getResourceAsStream("/faturas/" + name)) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

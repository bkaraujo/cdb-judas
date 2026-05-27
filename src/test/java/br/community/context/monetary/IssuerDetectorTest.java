package br.community.context.monetary;

import br.community.context.monetary._0_domain.model.Issuer;
import br.community.context.monetary._1_application.service.IssuerDetector;
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
    void returnsUnknownForUnrecognizedText() {
        assertEquals(Issuer.UNKNOWN, detector.detect("Documento sem marcador de banco algum."));
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = IssuerDetectorTest.class.getResourceAsStream("/faturas/" + name)) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

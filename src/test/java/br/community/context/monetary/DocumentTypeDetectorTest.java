package br.community.context.monetary;

import br.community.feature.user.accounts.statementimport.preview.DocumentType;
import br.community.feature.user.accounts.statementimport.preview.DocumentTypeDetector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Routing of an uploaded PDF to the invoice path vs the bank-statement path. */
class DocumentTypeDetectorTest {

    private final DocumentTypeDetector detector = new DocumentTypeDetector();

    @Test
    void detectsBtgBankStatementFromHeaderMarker() throws IOException {
        assertEquals(DocumentType.BANK_STATEMENT, detector.detect(resource("/extratos/extrato-btg-2025.txt")));
    }

    @Test
    void treatsCreditCardInvoicesAsInvoices() throws IOException {
        assertEquals(DocumentType.CREDIT_CARD_INVOICE, detector.detect(resource("/faturas/fatura-btg-maio.txt")));
        assertEquals(DocumentType.CREDIT_CARD_INVOICE, detector.detect(resource("/faturas/fatura-santander-maio.txt")));
    }

    @Test
    void recognizesStatementByCnpjEvenWithoutHeader() {
        assertEquals(DocumentType.BANK_STATEMENT,
                detector.detect("movimentações da conta CNPJ 30.306.294/0002-26 BTG"));
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = DocumentTypeDetectorTest.class.getResourceAsStream(path)) {
            return new String(Objects.requireNonNull(in, path).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

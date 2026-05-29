package br.community.context.monetary;

import br.commons.Result;
import br.commons.pdf.ExtractionFailure;
import br.commons.pdf.PdfBoxTextExtractor;
import br.community.PdfFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfBoxTextExtractorTest {

    private final PdfBoxTextExtractor extractor =
            new PdfBoxTextExtractor(50);

    @Test
    void extractsTextFromGoodPdf() throws Exception {
        var result = extractor.extract(PdfFixtures.withText("COMPRA TESTE 12345"), null);
        var value = assertInstanceOf(Result.Success.class, result).value();
        assertTrue(((String) value).contains("COMPRA TESTE 12345"));
    }

    @Test
    void encryptedWithoutPasswordReturnsEncrypted() throws Exception {
        var result = extractor.extract(PdfFixtures.encrypted("segredo", "qualquer"), null);
        var error = assertInstanceOf(Result.Failure.class, result).error();
        assertInstanceOf(ExtractionFailure.Encrypted.class, error);
    }

    @Test
    void encryptedWithWrongPasswordReturnsWrongPassword() throws Exception {
        var result = extractor.extract(PdfFixtures.encrypted("segredo", "qualquer"), "errada");
        var error = assertInstanceOf(Result.Failure.class, result).error();
        assertInstanceOf(ExtractionFailure.WrongPassword.class, error);
    }

    @Test
    void encryptedWithCorrectPasswordReturnsText() throws Exception {
        var result = extractor.extract(PdfFixtures.encrypted("segredo", "CONTEUDO SECRETO"), "segredo");
        var value = assertInstanceOf(Result.Success.class, result).value();
        assertTrue(((String) value).contains("CONTEUDO SECRETO"));
    }

    @Test
    void imageOnlyPdfReturnsNoTextLayer() throws Exception {
        var result = extractor.extract(PdfFixtures.imageOnly(), null);
        var error = assertInstanceOf(Result.Failure.class, result).error();
        assertInstanceOf(ExtractionFailure.NoTextLayer.class, error);
    }

    @Test
    void exceedingPageGuardReturnsTooManyPages() throws Exception {
        var smallGuard = new PdfBoxTextExtractor(2);
        var result = smallGuard.extract(PdfFixtures.withText("conteudo", 3), null);
        var error = assertInstanceOf(Result.Failure.class, result).error();
        assertInstanceOf(ExtractionFailure.TooManyPages.class, error);
    }
}

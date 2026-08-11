package br.commons.pdf;

import br.commons.Result;
import lombok.val;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class PdfBoxTextExtractorTest {

    private final PdfBoxTextExtractor extractor = new PdfBoxTextExtractor(50);

    @Test
    void extraiOCreationDateQuandoOPdfCarregaAMetadata() throws IOException {
        val createdAt = LocalDate.of(2025, 1, 7);
        switch (extractor.extract(pdf("hello world", createdAt), null)) {
            case Result.Success<ExtractedDocument, ExtractionFailure>(var doc) ->
                    assertEquals(createdAt, doc.createdAt());
            case Result.Failure<ExtractedDocument, ExtractionFailure> f -> fail("expected success: " + f.error());
        }
    }

    @Test
    void createdAtENuloQuandoOPdfNaoTemAMetadata() throws IOException {
        switch (extractor.extract(pdf("hello world", null), null)) {
            case Result.Success<ExtractedDocument, ExtractionFailure>(var doc) -> assertNull(doc.createdAt());
            case Result.Failure<ExtractedDocument, ExtractionFailure> f -> fail("expected success: " + f.error());
        }
    }

    private static byte[] pdf(String text, @Nullable LocalDate createdAt) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(20, 700);
                cs.showText(text);
                cs.endText();
            }
            if (createdAt != null) {
                doc.getDocumentInformation().setCreationDate(
                        GregorianCalendar.from(createdAt.atStartOfDay(ZoneId.systemDefault())));
            }
            val out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}

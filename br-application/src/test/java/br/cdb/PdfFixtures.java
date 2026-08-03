package br.cdb;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Builds synthetic PDFs in-memory so every import case can be exercised through the HTTP boundary
 * ({@code POST /accounts/transactions/import/preview}), which only accepts a PDF upload. Issue-01
 * committed the anonymized statements as plain text ({@code /faturas}, {@code /extratos}), so
 * {@link #fromResource} renders one back into a PDF.
 *
 * <p>The rendering is a verified round-trip: the text {@link PDFTextStripper} pulls out of the
 * generated PDF is line-for-line identical to the source (leading spaces included — Santander marks
 * continuation lines with them), the only difference being that blank lines vanish, which every
 * parser already skips. Page geometry is what makes that hold: the page is wide enough
 * ({@value #PAGE_WIDTH}pt) that no statement line wraps, and text is paginated at
 * {@value #LINES_PER_PAGE} lines so nothing is drawn off the bottom of a page.
 */
public final class PdfFixtures {

    private static final float PAGE_WIDTH = 2000f;
    private static final float PAGE_HEIGHT = 800f;
    private static final float LEFT_MARGIN = 20f;
    private static final float FIRST_BASELINE = 780f;
    private static final float LEADING = 10f;
    private static final float FONT_SIZE = 8f;
    private static final int LINES_PER_PAGE = 76;

    private PdfFixtures() {}

    /** PDF carrying the given (possibly multi-line) text, paginated as needed. */
    public static byte[] withText(String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            String[] lines = text.split("\\R", -1);
            for (int i = 0; i < lines.length; i += LINES_PER_PAGE) {
                writePage(doc, lines, i, Math.min(i + LINES_PER_PAGE, lines.length));
            }
            if (doc.getNumberOfPages() == 0) doc.addPage(new PDPage(pageSize()));
            return bytes(doc);
        }
    }

    /** {@code pages}-page PDF; the text is written on the first page, the rest are blank. */
    public static byte[] withText(String text, int pages) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            String[] lines = text.split("\\R", -1);
            writePage(doc, lines, 0, Math.min(LINES_PER_PAGE, lines.length));
            for (int i = 1; i < pages; i++) {
                doc.addPage(new PDPage(pageSize()));
            }
            return bytes(doc);
        }
    }

    /** The anonymized statement/invoice at {@code path} ({@code /faturas/…}, {@code /extratos/…}), as a PDF. */
    public static byte[] fromResource(String path) throws IOException {
        try (InputStream in = PdfFixtures.class.getResourceAsStream(path)) {
            return withText(new String(Objects.requireNonNull(in, path).readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /** Encrypted single-page PDF protected with {@code userPassword}. */
    public static byte[] encrypted(String userPassword, String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            writePage(doc, text.split("\\R", -1), 0, text.split("\\R", -1).length);

            AccessPermission permissions = new AccessPermission();
            StandardProtectionPolicy policy =
                    new StandardProtectionPolicy("owner-" + userPassword, userPassword, permissions);
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);

            return bytes(doc);
        }
    }

    /** Single-page PDF whose only content is a drawn image (no text layer). */
    public static byte[] imageOnly() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(pageSize());
            doc.addPage(page);

            BufferedImage image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, 60, 60);
            g.dispose();

            PDImageXObject xobject = LosslessFactory.createFromImage(doc, image);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(xobject, 100, 600, 60, 60);
            }
            return bytes(doc);
        }
    }

    /** The text the production extractor would read back out of {@code pdf} — round-trip guard. */
    public static String extract(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static PDRectangle pageSize() {
        return new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT);
    }

    private static void writePage(PDDocument doc, String[] lines, int from, int to) throws IOException {
        PDPage page = new PDPage(pageSize());
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
            cs.newLineAtOffset(LEFT_MARGIN, FIRST_BASELINE);
            for (int i = from; i < to; i++) {
                if (i > from) cs.newLineAtOffset(0, -LEADING);
                // Empty lines carry no glyph: the stripper simply omits them, which every parser
                // already tolerates (each one skips blank lines before matching).
                if (!lines[i].isEmpty()) cs.showText(lines[i]);
            }
            cs.endText();
        }
    }

    private static byte[] bytes(PDDocument doc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        return out.toByteArray();
    }
}

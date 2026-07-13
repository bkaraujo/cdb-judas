package br.cdb;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Builds synthetic PDFs in-memory for extractor/resource tests. Issue-01 committed only anonymized
 * text fixtures (no PDFs, none encrypted), so the encrypted / no-text-layer / too-many-pages code
 * paths are exercised against PDFs generated here.
 */
public final class PdfFixtures {

    private PdfFixtures() {}

    /** Single-page PDF carrying the given (possibly multi-line) text in its text layer. */
    public static byte[] withText(String text) throws IOException {
        return withText(text, 1);
    }

    /** {@code pages}-page PDF; the text is written on the first page, the rest are blank. */
    public static byte[] withText(String text, int pages) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                if (i == 0) {
                    writeText(doc, page, text);
                }
            }
            return bytes(doc);
        }
    }

    /** Encrypted single-page PDF protected with {@code userPassword}. */
    public static byte[] encrypted(String userPassword, String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            writeText(doc, page, text);

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
            PDPage page = new PDPage();
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

    private static void writeText(PDDocument doc, PDPage page, String text) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            cs.newLineAtOffset(50, 740);
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) cs.newLineAtOffset(0, -16);
                cs.showText(lines[i]);
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

package br.commons.pdf;

import br.commons.Result;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Sole owner of the Apache PDFBox dependency. Turns PDF bytes into text, mapping
 * failures to typed {@link ExtractionFailure}s. The password is a transient parameter only:
 * never persisted, never logged.
 */
@NullMarked
public final class PdfBoxTextExtractor implements PdfTextExtractor {

    private final int maxPages;

    public PdfBoxTextExtractor(int maxPages) {
        this.maxPages = maxPages;
    }

    @Override
    public Result<String, ExtractionFailure> extract(byte[] bytes, @Nullable String password) {
        final boolean hasPassword = password != null && !password.isBlank();
        try (PDDocument document = open(bytes, password, hasPassword)) {
            final int pages = document.getNumberOfPages();
            if (pages > maxPages) {
                return new Result.Failure<>(new ExtractionFailure.TooManyPages(pages, maxPages));
            }
            final String text = new PDFTextStripper().getText(document);
            if (text.isBlank()) {
                return new Result.Failure<>(new ExtractionFailure.NoTextLayer());
            }
            return Result.success(text);
        } catch (InvalidPasswordException e) {
            return new Result.Failure<>(hasPassword
                    ? new ExtractionFailure.WrongPassword()
                    : new ExtractionFailure.Encrypted());
        } catch (IOException e) {
            // Corrupt / not-a-PDF / otherwise unreadable → no usable text layer.
            return new Result.Failure<>(new ExtractionFailure.NoTextLayer());
        }
    }

    private static PDDocument open(byte[] bytes, @Nullable String password, boolean hasPassword) throws IOException {
        if (hasPassword && password != null) {
            return Loader.loadPDF(bytes, password);
        }
        return Loader.loadPDF(bytes);
    }
}

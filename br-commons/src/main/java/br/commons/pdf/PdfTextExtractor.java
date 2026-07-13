package br.commons.pdf;

import br.commons.Result;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Generic technical port: turns raw PDF bytes (optionally password-protected) into text.
 * The single owner of the PDF-parsing technology is {@link PdfBoxTextExtractor}.
 */
@NullMarked
public interface PdfTextExtractor {

    Result<String, ExtractionFailure> extract(byte[] bytes, @Nullable String password);
}

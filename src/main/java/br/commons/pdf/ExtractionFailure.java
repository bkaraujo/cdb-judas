package br.commons.pdf;

import org.jspecify.annotations.NullMarked;

/** Typed outcomes when a PDF cannot be turned into usable text. */
@NullMarked
public sealed interface ExtractionFailure {

    /** PDF is encrypted and no password was supplied. */
    @NullMarked
    record Encrypted() implements ExtractionFailure {}

    /** PDF is encrypted and the supplied password did not open it. */
    @NullMarked
    record WrongPassword() implements ExtractionFailure {}

    /** PDF opened but carries no extractable text layer (scanned/image-only or unreadable). */
    @NullMarked
    record NoTextLayer() implements ExtractionFailure {}

    /** PDF exceeds the page-count guard. */
    @NullMarked
    record TooManyPages(int pages, int maxPages) implements ExtractionFailure {}
}

package br.commons.pdf;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * Successful extraction result: the PDF's text layer plus its own {@code /CreationDate} metadata
 * (from {@code PDDocumentInformation}), when present. {@code createdAt} is a secondary anchor for
 * callers that need to date a document whose printed text carries no year of its own (e.g. an older
 * invoice template) — see {@code StatementImportService}.
 */
@NullMarked
public record ExtractedDocument(String text, @Nullable LocalDate createdAt) {}

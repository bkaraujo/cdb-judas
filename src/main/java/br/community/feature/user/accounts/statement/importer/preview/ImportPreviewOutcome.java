package br.community.feature.user.accounts.statement.importer.preview;

import org.jspecify.annotations.NullMarked;

/**
 * The result of a preview, after document-type routing: either a credit-card {@link Invoice} preview
 * (matched card + expanded charges) or a {@link Statement} preview (destination accounts + signed
 * movements). The HTTP layer renders each shape to its own JSON, tagged with the document type.
 */
@NullMarked
public sealed interface ImportPreviewOutcome {

    @NullMarked
    record Invoice(ImportPreview preview) implements ImportPreviewOutcome {}

    @NullMarked
    record Statement(BankStatementPreview preview) implements ImportPreviewOutcome {}
}

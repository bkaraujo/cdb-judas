package br.community.feature.user.accounts.transactions.importer;

import br.community.feature.user.accounts.transactions.importer.preview.ImportPreviewOutcome;
import br.community.feature.user.accounts.transactions.importer.preview.Issuer;
import br.community.feature.user.accounts.transactions.importer.preview.ParsedStatementLine;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * The common parse result of a
 * {@link br.community.feature.user.accounts.transactions.importer.StatementParser}: either a credit-card
 * {@link Invoice} or a checking-account {@link Statement}. Each variant carries the detected
 * {@link Issuer} (surfaced as the JSON {@code issuer}) plus the bank-specific parsed payload; the use
 * case switches on the variant to drive the matching preview pipeline. This sealed split mirrors
 * {@link ImportPreviewOutcome} at the parse stage, so the JSON the resource renders is unchanged.
 */
@NullMarked
public sealed interface MonetaryDocument {

    Issuer issuer();

    @NullMarked
    record Invoice(Issuer issuer, List<ParsedStatementLine> statement) implements MonetaryDocument {}

    @NullMarked
    record Statement(Issuer issuer, List<ParsedStatementLine> statement) implements MonetaryDocument {}
}

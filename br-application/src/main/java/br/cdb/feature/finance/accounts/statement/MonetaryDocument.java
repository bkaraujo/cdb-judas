package br.cdb.feature.finance.accounts.statement;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * The common parse result of a
 * {@link StatementParser}: either a credit-card
 * {@link Invoice} or a checking-account {@link Statement}. Each variant carries the detected
 * {@link Issuer} (surfaced as the JSON {@code issuer}) plus the bank-specific parsed payload; the use
 * case switches on the variant to drive the matching preview pipeline.
 */
@NullMarked
public sealed interface MonetaryDocument {

    String issuer();

    @NullMarked
    record Invoice(String issuer, List<MonetaryDocumentEntry> statement) implements MonetaryDocument {}

    @NullMarked
    record Statement(String issuer, List<MonetaryDocumentEntry> statement) implements MonetaryDocument {}
}

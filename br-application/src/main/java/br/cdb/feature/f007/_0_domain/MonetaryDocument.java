package br.cdb.feature.f007._0_domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
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

    /** {@code period} is the invoice's own printed month/year; {@code null} if the parser couldn't find it. */
    @NullMarked
    record Invoice(String issuer, @Nullable YearMonth period, List<MonetaryDocumentEntry> statement) implements MonetaryDocument {}

    @NullMarked
    record Statement(String issuer, List<MonetaryDocumentEntry> statement) implements MonetaryDocument {}
}

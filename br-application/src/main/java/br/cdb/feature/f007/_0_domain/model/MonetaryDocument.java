package br.cdb.feature.f007._0_domain.model;

import br.cdb.feature.f007._1_application.StatementParser;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

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

    /**
     * {@code period} is the invoice's own printed month/year; {@code null} if the parser couldn't find
     * it. {@code printedTotals} is the invoice's own per-card checksum ({@code last4 → "Total do
     * cartão"} as printed), used only to log a reconciliation warning — empty when the parser doesn't
     * capture it (e.g. Santander) or couldn't read a given card's value.
     */
    @NullMarked
    record Invoice(String issuer, @Nullable YearMonth period, List<MonetaryDocumentEntry> statement,
                    Map<String, BigDecimal> printedTotals) implements MonetaryDocument {}

    @NullMarked
    record Statement(String issuer, List<MonetaryDocumentEntry> statement) implements MonetaryDocument {}
}

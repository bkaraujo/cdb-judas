package br.community.feature.user.accounts.statement.importer.preview;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;

/**
 * One kept line of a parsed statement — a credit-card charge or a checking-account movement.
 *
 * <p>{@code date} is always a {@link LocalDate}. For a bank movement it is the absolute date the bank
 * prints ({@code dd/MM/yyyy}). A credit-card invoice prints day/month only, so for a charge the year
 * is a {@link #charge placeholder}: the real year is inferred later by the InstallmentExpander
 * (statement period + installment offset), which reads only the month/day and never the placeholder.
 *
 * <p>{@code installmentNumber}/{@code installmentTotal} are present only for parcelado card lines
 * ("n/N"); à-vista charges and bank movements carry {@code null} for both. {@code last4} carries the
 * card for a charge and is {@code null} for a bank movement.
 */
@NullMarked
public record ParsedStatementLine(
        @Nullable String last4,
        LocalDate date,
        String description,
        BigDecimal amount,
        @Nullable Integer installmentNumber,
        @Nullable Integer installmentTotal,
        ChargeKind kind
) {
    /**
     * Placeholder year stamped on a credit-card charge's year-less {@link MonthDay}. A leap year so
     * 29 Feb purchases are representable; its value is irrelevant downstream — the InstallmentExpander
     * reads only the month/day and resolves the real year from the statement period.
     */
    private static final int CARD_PLACEHOLDER_YEAR = 2000;

    /** A checking-account movement: absolute date, no card, no installment. */
    public ParsedStatementLine(LocalDate date, String description, BigDecimal amount) {
        this(null, date, description, amount, null, null, ChargeKind.PURCHASE);
    }

    /** A credit-card charge printed year-less ({@link MonthDay}); the year is a {@link #CARD_PLACEHOLDER_YEAR placeholder}. */
    public static ParsedStatementLine charge(@Nullable String last4, MonthDay date, String description,
                                             BigDecimal amount, @Nullable Integer installmentNumber,
                                             @Nullable Integer installmentTotal, ChargeKind kind) {
        return new ParsedStatementLine(last4, date.atYear(CARD_PLACEHOLDER_YEAR), description, amount,
                installmentNumber, installmentTotal, kind);
    }

    public boolean isInstallment() {
        return installmentNumber != null && installmentTotal != null;
    }
}

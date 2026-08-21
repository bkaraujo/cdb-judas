package br.cdb.feature.f007._0_domain.model;

import br.cdb.feature.f005._0_domain.model.Nature;
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
 * <p>{@code installmentNumber}/{@code installmentTotal} carry the "n/N" of a parcelado card line;
 * à-vista charges and bank movements are a single installment ({@code 1}/{@code 1}), so
 * {@link #isInstallment()} (total &gt; 1) tells the two apart. {@code last4} carries the
 * card for a charge and is {@code null} for a bank movement. {@code amount} is always the positive
 * BRL value as printed; {@code nature} carries the sign instead — a card credit (refund/estorno) is
 * {@code INCOME}, everything else {@code EXPENSE}. A credit never installments (always {@code 1/1}).
 */
@NullMarked
public record MonetaryDocumentEntry(
        @Nullable String last4,
        LocalDate date,
        String description,
        BigDecimal amount,
        int installmentNumber,
        int installmentTotal,
        ChargeKind kind,
        Nature type
) {
    /**
     * Placeholder year stamped on a credit-card charge's year-less {@link MonthDay}. A leap year so
     * 29 Feb purchases are representable; its value is irrelevant downstream — the InstallmentExpander
     * reads only the month/day and resolves the real year from the statement period.
     */
    private static final int CARD_PLACEHOLDER_YEAR = 2000;

    /** A checking-account movement: absolute date, no card, no installment. */
    public MonetaryDocumentEntry(LocalDate date, String description, BigDecimal amount) {
        this(null, date, description, amount, 1, 1, ChargeKind.PURCHASE,
                amount.signum() < 0 ? Nature.EXPENSE : Nature.INCOME);
    }

    /** A credit-card {@code EXPENSE} charge printed year-less ({@link MonthDay}); the year is a {@link #CARD_PLACEHOLDER_YEAR placeholder}. */
    public static MonetaryDocumentEntry charge(@Nullable String last4, MonthDay date, String description, BigDecimal amount, int installmentNumber, int installmentTotal, ChargeKind kind) {
        return charge(last4, date, description, amount, installmentNumber, installmentTotal, kind, Nature.EXPENSE);
    }

    /** A credit-card charge printed year-less ({@link MonthDay}); the year is a {@link #CARD_PLACEHOLDER_YEAR placeholder}. */
    public static MonetaryDocumentEntry charge(@Nullable String last4, MonthDay date, String description, BigDecimal amount, int installmentNumber, int installmentTotal, ChargeKind kind, Nature type) {
        return new MonetaryDocumentEntry(last4, date.atYear(CARD_PLACEHOLDER_YEAR), description, amount, installmentNumber, installmentTotal, kind, type);
    }

    /** A credit-card {@code EXPENSE} charge printed year-less ({@link MonthDay}); the year is a {@link #CARD_PLACEHOLDER_YEAR placeholder}. */
    public static MonetaryDocumentEntry charge(@Nullable String last4, MonthDay date, String description, BigDecimal amount, ChargeKind kind) {
        return charge(last4, date, description, amount, 1, 1, kind);
    }

    /** À-vista card credit (refund/estorno): always {@code 1/1}, never installments. */
    public static MonetaryDocumentEntry credit(@Nullable String last4, MonthDay date, String description, BigDecimal amount, ChargeKind kind) {
        return charge(last4, date, description, amount, 1, 1, kind, Nature.INCOME);
    }

    public boolean isInstallment() {
        return installmentTotal > 1;
    }
}

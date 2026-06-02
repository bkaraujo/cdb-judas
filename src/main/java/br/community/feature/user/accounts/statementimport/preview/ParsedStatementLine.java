package br.community.feature.user.accounts.statementimport.preview;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.MonthDay;

/**
 * One kept charge from a credit-card statement, as printed by the bank.
 *
 * <p>The purchase date is year-less ({@link MonthDay}) because both banks print day/month only;
 * the absolute date and installment schedule are inferred later by the InstallmentExpander.
 * {@code installmentNumber}/{@code installmentTotal} are present only for parcelado lines ("n/N");
 * à-vista lines carry {@code null} for both.
 */
@NullMarked
public record ParsedStatementLine(
        String last4,
        MonthDay date,
        String description,
        BigDecimal amount,
        @Nullable Integer installmentNumber,
        @Nullable Integer installmentTotal,
        ChargeKind kind
) {
    public boolean isInstallment() {
        return installmentNumber != null && installmentTotal != null;
    }
}

package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One expanded charge before persistence. À-vista lines yield a single draft; parcelado lines yield
 * one draft per installment, all sharing the same {@code groupId}.
 *
 * <p>{@code date} is this installment's absolute date; {@code originalDate} is the original purchase
 * date, carried so confirm can recompute the group signature. {@code amount} is the positive BRL
 * value as parsed (the persistence sign is applied by a later slice). {@code type} is always
 * {@code "expense"}; {@code status} is {@code "confirmed"} (this month or earlier) or
 * {@code "scheduled"} (future month).
 */
@NullMarked
public record TransactionDraft(
        String last4,
        LocalDate date,
        LocalDate originalDate,
        String description,
        BigDecimal amount,
        String status,
        String type,
        UUID accountId,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer installmentTotal,
        ChargeKind kind
) {}

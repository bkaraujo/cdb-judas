package br.community.context.monetary._1_application.command;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Persist one already-resolved imported credit-card charge. {@code amount} is the POSITIVE charge
 * value — the server applies the expense sign. {@code status} ({@code "confirmed"}/{@code "scheduled"})
 * and the optional {@code groupId}/installment fields are computed by the import orchestration and
 * stored as given.
 */
@NullMarked
public record ImportedTransactionCommand(
        UUID accountId,
        String description,
        BigDecimal amount,
        LocalDate date,
        UUID categoryId,
        String status,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments) {}

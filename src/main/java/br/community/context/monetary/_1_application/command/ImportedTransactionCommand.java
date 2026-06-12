package br.community.context.monetary._1_application.command;

import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Persist one already-resolved imported movement. {@code amount} is the POSITIVE value — the server
 * applies the sign from {@code type} ({@code EXPENSE} → negative, {@code INCOME} → positive).
 * {@code status} ({@code CONFIRMED}/{@code SCHEDULED}) and the optional {@code groupId}/installment
 * fields are computed by the import orchestration and stored as given. Credit-card invoices always
 * pass {@code EXPENSE}; bank statements pass the type derived from the printed sign.
 */
@NullMarked
public record ImportedTransactionCommand(
        UUID accountId,
        String description,
        BigDecimal amount,
        LocalDate date,
        UUID categoryId,
        MonetaryTransaction.Status status,
        MonetaryTransaction.Type type,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments) {}

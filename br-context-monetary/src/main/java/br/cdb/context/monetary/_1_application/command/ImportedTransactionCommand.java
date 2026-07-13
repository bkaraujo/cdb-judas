package br.cdb.context.monetary._1_application.command;

import br.cdb.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Persist one already-resolved imported movement. {@code amount} is the signed value — the server
 * derives the signal from the sign. {@code status} ({@code CONFIRMED}/{@code SCHEDULED}) and the
 * optional {@code groupId}/installment fields are computed by the import orchestration.
 */
@NullMarked
public record ImportedTransactionCommand(
        UUID accountId,
        String description,
        BigDecimal amount,
        LocalDate date,
        Transaction.Status status,
        Transaction.Type type,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments,
        @Nullable UUID cardId) {}

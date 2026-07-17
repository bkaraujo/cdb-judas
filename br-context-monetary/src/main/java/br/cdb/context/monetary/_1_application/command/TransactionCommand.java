package br.cdb.context.monetary._1_application.command;

import br.cdb.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@NullMarked
public sealed interface TransactionCommand {

    @NullMarked
    sealed interface Upsert extends TransactionCommand {}

    @NullMarked
    record Create(
            String description,
            BigDecimal amount,
            LocalDate date,
            UUID accountId,
            UUID costCenterId,
            Transaction.Status status,
            Transaction.Type type,
            @Nullable Integer installments,
            @Nullable String notes,
            @Nullable UUID cardId
    ) implements Upsert {}

    @NullMarked
    record Update(
            UUID id,
            String description,
            BigDecimal amount,
            LocalDate date,
            UUID accountId,
            UUID costCenterId,
            Transaction.Status status,
            Transaction.Type type,
            TransactionScope scope,
            @Nullable String notes,
            @Nullable UUID cardId
    ) implements Upsert {}

    @NullMarked
    record Delete(
            UUID id,
            TransactionScope scope
    ) implements TransactionCommand {}


    /**
     * Persist one already-resolved imported movement. {@code amount} is the signed value — the server
     * derives the signal from the sign. {@code status} ({@code CONFIRMED}/{@code SCHEDULED}) and the
     * optional {@code groupId}/installment fields are computed by the import orchestration.
     */
    @NullMarked
    record Import(
            UUID accountId,
            String description,
            BigDecimal amount,
            LocalDate date,
            Transaction.Status status,
            Transaction.Type type,
            @Nullable UUID groupId,
            @Nullable Integer installmentNumber,
            @Nullable Integer totalInstallments,
            @Nullable UUID cardId
    ) {}

    /**
     * Confirm a credit-card statement import: the rows the user kept. Deselected rows are simply absent.
     * {@code amount} is the POSITIVE charge value — the server applies the expense sign. Status is
     * recomputed server-side from {@code date}; no client status is trusted.
     *
     * <p>A single invoice can carry charges from several cards (titular + additional cardholders), so each
     * {@link Row} names its own {@code cardId}. The persistence account (and therefore the dedup/group
     * identity) is resolved per row from that card.
     */
    @NullMarked
    record ImportConfirm(List<Row> rows) {

        @NullMarked
        public record Row(
                String description,
                BigDecimal amount,
                LocalDate date,
                LocalDate originalDate,
                @Nullable Integer installmentNumber,
                @Nullable Integer installmentTotal,
                UUID categoryId,
                UUID cardId
        ) {}
    }

}

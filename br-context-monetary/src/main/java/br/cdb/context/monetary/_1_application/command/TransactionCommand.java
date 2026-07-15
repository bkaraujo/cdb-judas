package br.cdb.context.monetary._1_application.command;

import br.cdb.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
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

}

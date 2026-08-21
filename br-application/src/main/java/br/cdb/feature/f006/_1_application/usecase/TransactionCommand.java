package br.cdb.feature.f006._1_application.usecase;

import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006._0_domain.model.Transaction;
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
            Nature type,
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
            Nature type,
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

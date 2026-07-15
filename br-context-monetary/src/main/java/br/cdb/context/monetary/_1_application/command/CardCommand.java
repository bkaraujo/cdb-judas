package br.cdb.context.monetary._1_application.command;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public sealed interface CardCommand {

    @NullMarked
    sealed interface Upsert extends CardCommand {}

    @NullMarked
    record Create(
            UUID accountId,
            String last4
    ) implements Upsert {}

    @NullMarked
    record Update(
            UUID id,
            boolean active
    ) implements Upsert {}

    @NullMarked
    record Delete(
            UUID id,
            TransactionPolicy policy
    ) implements CardCommand {}

}

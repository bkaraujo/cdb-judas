package br.cdb.context.monetary._1_application.command;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public sealed interface CostCenterCommand {

    @NullMarked
    sealed interface Upsert extends CostCenterCommand {}

    @NullMarked
    record Create(
            String description
    ) implements Upsert {}
    @NullMarked
    record Update(
            UUID id,
            String description
    ) implements Upsert {}
    @NullMarked
    record Delete(
            UUID id
    ) implements CostCenterCommand {}


}

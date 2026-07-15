package br.cdb.context.monetary._1_application.command;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public sealed interface CostCenterCommand {

    sealed interface Upsert extends CostCenterCommand {}

    @NullMarked
    record Create(
            @NotBlank String description
    ) implements Upsert {}
    @NullMarked
    record Update(
            UUID id,
            @NotBlank String description
    ) implements Upsert {}
    @NullMarked
    record Delete(
            UUID id
    ) implements CostCenterCommand {}


}

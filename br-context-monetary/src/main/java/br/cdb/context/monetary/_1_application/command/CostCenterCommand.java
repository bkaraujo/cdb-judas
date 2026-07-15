package br.cdb.context.monetary._1_application.command;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface CostCenterCommand {


    @NullMarked
    record Create(
            @NotBlank String description
    ) implements CostCenterCommand {}
    @NullMarked
    record Update(
            UUID id,
            @NotBlank String description
    ) implements CostCenterCommand {}
    @NullMarked
    record Delete(
            UUID id
    ) implements CostCenterCommand {}


}

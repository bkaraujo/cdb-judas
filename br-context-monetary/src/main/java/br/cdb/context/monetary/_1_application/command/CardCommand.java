package br.cdb.context.monetary._1_application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public sealed interface CardCommand {

    sealed interface Upsert extends CardCommand {}

    @NullMarked
    record Create(
            @NotNull UUID accountId,
            @NotBlank @Pattern(regexp = "\\d{4}") String last4
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

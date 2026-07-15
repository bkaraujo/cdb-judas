package br.cdb.context.monetary._1_application.command;

import br.commons.validation.TwoDecimalPlaces;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@NullMarked
public sealed interface AccountCommand {

    sealed interface Upsert extends AccountCommand {}

    @NullMarked
    record Create(
            @NotBlank String name,
            @NotBlank String type,
            boolean active,
            @Nullable @PositiveOrZero @TwoDecimalPlaces BigDecimal creditLimit,
            @Nullable @PositiveOrZero @TwoDecimalPlaces BigDecimal overdraftLimit,
            @Nullable @Min(1) @Max(31) Integer closingDay,
            @Nullable @Min(1) @Max(31) Integer dueDay
    ) implements Upsert {}

    @NullMarked
    record Update(
            UUID id,
            @NotBlank String name,
            @NotBlank String type,
            boolean active,
            @Nullable @PositiveOrZero @TwoDecimalPlaces BigDecimal creditLimit,
            @Nullable @PositiveOrZero @TwoDecimalPlaces BigDecimal overdraftLimit,
            @Nullable @Min(1) @Max(31) Integer closingDay,
            @Nullable @Min(1) @Max(31) Integer dueDay
    ) implements Upsert {}

    @NullMarked
    record Delete(
            UUID id,
            TransactionPolicy policy
    ) implements AccountCommand {}

}

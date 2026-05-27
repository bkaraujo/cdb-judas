package br.community.context.monetary._1_application.command;

import br.community.core.TwoDecimalPlaces;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NullMarked
public record TransactionCommand(
        @NotBlank String description,
        @NotNull @TwoDecimalPlaces BigDecimal amount,
        @NotNull LocalDate date,
        @NotNull UUID categoryId,
        @NotNull UUID accountId,
        @NotBlank String status,
        @NotBlank String type,
        @Nullable @Min(1) Integer installments,
        @Nullable String editMode
) {}

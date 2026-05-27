package br.community.context.monetary._1_application.command;

import br.community.core.TwoDecimalPlaces;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.util.UUID;

@NullMarked
public record CreditCardCommand(
        @NotNull UUID accountId,
        @NotNull String name,
        @NotBlank @Pattern(regexp = "\\d{4}") String last4,
        @NotNull @PositiveOrZero @TwoDecimalPlaces BigDecimal limit,
        @Min(1) @Max(31) int closingDay,
        @Min(1) @Max(31) int dueDay,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
        boolean active
) {}

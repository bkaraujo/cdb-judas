package br.community.context.monetary._1_application.command;

import br.community.core.TwoDecimalPlaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;

@NullMarked
public record AccountCommand(
        @NotBlank String name,
        @NotNull @TwoDecimalPlaces BigDecimal balance,
        @NotBlank String type,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
        boolean active
) {}

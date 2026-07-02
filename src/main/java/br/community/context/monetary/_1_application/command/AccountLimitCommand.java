package br.community.context.monetary._1_application.command;

import br.community.core.TwoDecimalPlaces;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

@NullMarked
public record AccountLimitCommand(
        @Nullable @PositiveOrZero @TwoDecimalPlaces BigDecimal creditLimit,
        @Nullable @PositiveOrZero @TwoDecimalPlaces BigDecimal overdraftLimit,
        @Nullable @Min(1) @Max(31) Integer closingDay,
        @Nullable @Min(1) @Max(31) Integer dueDay
) {}

package br.community.feature.user.accounts.core;

import br.community.core.TwoDecimalPlaces;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@NullMarked
public record AccountRequest(
        @NotBlank String name,
        @NotNull @TwoDecimalPlaces BigDecimal balance,
        @NotBlank String type,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
        boolean active,
        @Nullable UUID linkedAccountId,
        @Nullable @Pattern(regexp = "\\d{4}") String last4,
        @Nullable @Min(1) @Max(31) Integer dueDay,
        @Nullable @Min(1) @Max(31) Integer closingDay,
        @Nullable @PositiveOrZero @TwoDecimalPlaces BigDecimal creditLimit,
        @Nullable @PositiveOrZero @TwoDecimalPlaces BigDecimal overdraftLimit
) {}

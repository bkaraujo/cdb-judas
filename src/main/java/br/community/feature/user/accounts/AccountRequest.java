package br.community.feature.user.accounts;

import br.community.core.TwoDecimalPlaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@NullMarked
public record AccountRequest(
        @NotBlank String name,
        @NotNull @TwoDecimalPlaces BigDecimal balance,
        @NotBlank String type,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
        boolean active,
        @Nullable UUID linkedAccountId,
        @Nullable Map<String, Object> additionalInfo
) {}

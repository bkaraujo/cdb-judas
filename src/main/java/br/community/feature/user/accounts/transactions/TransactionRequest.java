package br.community.feature.user.accounts.transactions;

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
public record TransactionRequest(
        @NotBlank String description,
        @NotNull @TwoDecimalPlaces BigDecimal amount,
        @NotNull LocalDate date,
        @NotNull UUID categoryId,
        @Nullable UUID accountId,
        @NotNull UUID costCenterId,
        @NotBlank String status,
        @NotBlank String type,
        @Nullable @Min(1) Integer installments,
        @Nullable String editMode,
        @Nullable String deleteMode
) {}

package br.cdb.feature.f005._2_infrastructure.web.request;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.commons.validation.TwoDecimalPlaces;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        @NotNull Transaction.Status status,
        @NotNull Transaction.Type type,
        @Nullable @Min(1) Integer installments,
        @Nullable String editMode,
        @Nullable String deleteMode,
        @Nullable @Size(max = 250) String notes,
        @Nullable UUID cardId
) {}

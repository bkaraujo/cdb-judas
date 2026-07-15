package br.cdb.context.monetary._1_application.command;

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
public interface TransactionCommand {

    @NullMarked
    record Create(
            @NotBlank String description,
            @NotNull @TwoDecimalPlaces BigDecimal amount,
            @NotNull LocalDate date,
            @NotNull UUID accountId,
            @NotNull UUID costCenterId,
            @NotNull Transaction.Status status,
            @NotNull Transaction.Type type,
            @Nullable @Min(1) Integer installments,
            @Nullable @Size(max = 250) String notes,
            @Nullable UUID cardId
    ) implements TransactionCommand {}

    @NullMarked
    record Update(
            UUID id,
            @NotBlank String description,
            @NotNull @TwoDecimalPlaces BigDecimal amount,
            @NotNull LocalDate date,
            @NotNull UUID accountId,
            @NotNull UUID costCenterId,
            @NotNull Transaction.Status status,
            @NotNull Transaction.Type type,
            @Nullable String editMode,
            @Nullable @Size(max = 250) String notes,
            @Nullable UUID cardId
    ) implements TransactionCommand {}

    @NullMarked
    record Delete(
            UUID id,
            @Nullable String mode
    ) implements TransactionCommand {}

}

package br.community.feature.user.accounts.transactions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Confirm-import payload: the chosen destination card and the kept rows. {@code amount} is the
 * POSITIVE charge value (the server applies the expense sign); the client status is ignored
 * (recomputed server-side from {@code date}). Deselected rows are simply omitted by the client.
 */
@NullMarked
public record ImportConfirmRequest(@NotNull UUID cardId, @NotNull @Valid List<Row> rows) {

    @NullMarked
    public record Row(
            @NotBlank String description,
            @NotNull BigDecimal amount,
            @NotNull LocalDate date,
            @NotNull LocalDate originalDate,
            @Nullable Integer installmentNumber,
            @Nullable Integer installmentTotal,
            @NotNull UUID categoryId
    ) {}
}

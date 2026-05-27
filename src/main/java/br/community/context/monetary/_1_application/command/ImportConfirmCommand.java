package br.community.context.monetary._1_application.command;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Confirm a credit-card statement import: the chosen destination card and the rows the user kept.
 * Deselected rows are simply absent. {@code amount} is the POSITIVE charge value — the server applies
 * the expense sign. Status is recomputed server-side from {@code date}; no client status is trusted.
 */
@NullMarked
public record ImportConfirmCommand(@NotNull UUID cardId, @NotNull List<Row> rows) {

    @NullMarked
    public record Row(
            String description,
            BigDecimal amount,
            LocalDate date,
            LocalDate originalDate,
            @Nullable Integer installmentNumber,
            @Nullable Integer installmentTotal,
            UUID categoryId
    ) {}
}

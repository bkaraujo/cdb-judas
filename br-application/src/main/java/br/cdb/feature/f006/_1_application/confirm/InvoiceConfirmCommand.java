package br.cdb.feature.f006._1_application.confirm;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Confirm a credit-card statement import: the rows the user kept. Deselected rows are simply absent.
 * {@code amount} is the POSITIVE charge value — the server applies the expense sign. Status is
 * recomputed server-side from {@code date}; no client status is trusted.
 *
 * <p>A single invoice can carry charges from several cards (titular + additional cardholders), so each
 * {@link Row} names its own {@code cardId}. The persistence account (and therefore the dedup/group
 * identity) is resolved per row from that card.
 */
@NullMarked
public record InvoiceConfirmCommand(List<Row> rows) {

    @NullMarked
    public record Row(
            String description,
            BigDecimal amount,
            LocalDate date,
            LocalDate originalDate,
            @Nullable Integer installmentNumber,
            @Nullable Integer installmentTotal,
            UUID categoryId,
            UUID cardId
    ) {}
}

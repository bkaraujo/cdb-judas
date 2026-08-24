package br.cdb.feature.f007._1_application.confirm;

import br.cdb.feature.f005._0_domain.model.Nature;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Confirm a credit-card statement import: the rows the user kept. Deselected rows are simply absent.
 * {@code amount} is the POSITIVE charge/credit value — the server applies the sign from {@code nature}
 * ({@code EXPENSE} when the api omits it, matching every row before card credits were surfaced).
 * Status is recomputed server-side from {@code date}; no api status is trusted.
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
            @Nullable Nature type,
            UUID categoryId,
            boolean planned,
            UUID cardId,
            List<UUID> tagIds
    ) {}
}

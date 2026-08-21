package br.cdb.feature.f007;

import br.cdb.core.View;
import br.cdb.feature.f005._0_domain.model.Nature;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Cliente da API pública de {@code f007} */
@NullMarked
public interface F007Api {

    enum StatementType { CREDIT_CARD_INVOICE, BANK_STATEMENT }

    /** Espelha {@code StatementConfirmRequest.Row}. */
    @NullMarked
    record ConfirmRow(
            String description,
            BigDecimal amount,
            LocalDate date,
            @Nullable LocalDate originalDate,
            @Nullable Integer installmentNumber,
            @Nullable Integer installmentTotal,
            @Nullable Nature transactionType,
            UUID categoryId,
            @Nullable UUID costCenterId,
            @Nullable UUID cardId,
            @Nullable List<UUID> tagIds
    ) implements View {}

    /** Corpo de {@link #confirm} — espelha {@code StatementConfirmRequest}. */
    @NullMarked
    record ConfirmBody(StatementType type, @Nullable UUID accountId, List<ConfirmRow> rows) {}

    /** Espelha {@code ImportConfirmResponse}. */
    @NullMarked
    record ConfirmResult(int created, int reconciled, int skipped) implements View {}

    ConfirmResult confirm(ConfirmBody body);

}

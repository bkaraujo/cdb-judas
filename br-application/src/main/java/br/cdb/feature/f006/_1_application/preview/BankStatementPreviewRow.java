package br.cdb.feature.f006._1_application.preview;

import br.cdb.feature.f006._0_domain.RowState;
import br.cdb.feature.f006._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One preview row of a bank-statement import. {@code amount} is signed (debit negative, credit
 * positive) and {@code type} is derived from that sign ({@code EXPENSE}/{@code INCOME}).
 * {@code state} reflects the row against the chosen account (see {@link RowState}); when it is
 * {@link RowState#RECONCILE}, {@code reconcileDescription} carries the matched manual transaction's
 * description so the UI can show what it will confirm. {@code categoryId} is the guessed category
 * (used only when the row is inserted).
 */
@NullMarked
public record BankStatementPreviewRow(
        LocalDate date,
        String description,
        BigDecimal amount,
        Transaction.Type type,
        RowState state,
        @Nullable UUID categoryId,
        @Nullable String reconcileDescription) {}

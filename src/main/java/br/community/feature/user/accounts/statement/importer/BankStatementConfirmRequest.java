package br.community.feature.user.accounts.statement.importer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Confirm-import payload for a bank statement: the chosen destination account and the kept rows.
 * {@code amount} is signed (debit negative, credit positive) and {@code type} mirrors the sign. The
 * server re-derives each row's fate (insert / reconcile / skip); deselected rows are omitted.
 */
@NullMarked
public record BankStatementConfirmRequest(@NotNull UUID accountId, @NotNull @Valid List<Row> rows) {

    @NullMarked
    public record Row(
            @NotBlank String description,
            @NotNull BigDecimal amount,
            @NotNull LocalDate date,
            @NotBlank String type,
            @NotNull UUID categoryId
    ) {}
}

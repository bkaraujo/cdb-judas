package br.community.feature.user.accounts.statementimport.confirm;

import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Confirm a bank-statement import: the chosen destination account and the rows the user kept.
 * {@code amount} is signed (debit negative, credit positive); {@code type} mirrors the sign. The
 * server re-derives each row's fate (insert / reconcile / skip) against the account's current
 * transactions, so the import is idempotent and authoritative regardless of the preview.
 */
@NullMarked
public record BankStatementConfirmCommand(UUID accountId, List<Row> rows) {

    @NullMarked
    public record Row(
            String description,
            BigDecimal amount,
            LocalDate date,
            String type,
            UUID categoryId) {}
}

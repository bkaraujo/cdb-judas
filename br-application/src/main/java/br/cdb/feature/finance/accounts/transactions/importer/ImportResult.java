package br.cdb.feature.finance.accounts.transactions.importer;

import org.jspecify.annotations.NullMarked;

/**
 * Outcome of a confirmed import: how many transactions were persisted ({@code created}), how many
 * existing manual transactions were reconciled (promoted to {@code confirmed}, only the bank-statement
 * path) and how many rows were recognized as already imported and skipped ({@code skipped}).
 */
@NullMarked
public record ImportResult(int created, int reconciled, int skipped) {}

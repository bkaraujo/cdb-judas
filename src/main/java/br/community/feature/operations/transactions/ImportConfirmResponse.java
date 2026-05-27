package br.community.feature.operations.transactions;

import org.jspecify.annotations.NullMarked;

/**
 * Confirm-import outcome: number of transactions persisted ({@code created}) and number of rows
 * recognized as already imported and skipped ({@code skipped}).
 */
@NullMarked
public record ImportConfirmResponse(int created, int skipped) {}

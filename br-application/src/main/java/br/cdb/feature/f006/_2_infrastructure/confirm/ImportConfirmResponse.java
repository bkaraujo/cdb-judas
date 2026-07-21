package br.cdb.feature.f006._2_infrastructure.confirm;

import org.jspecify.annotations.NullMarked;

/**
 * Confirm-import outcome: number of transactions persisted ({@code created}), number of existing
 * manual transactions reconciled ({@code reconciled}, bank-statement path only) and number of rows
 * recognized as already imported and skipped ({@code skipped}).
 */
@NullMarked
public record ImportConfirmResponse(int created, int reconciled, int skipped) {}

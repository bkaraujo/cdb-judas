package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;

/**
 * Outcome of a confirmed credit-card statement import: how many transactions were persisted
 * ({@code created}) and how many were recognized as already imported and skipped ({@code skipped}).
 */
@NullMarked
public record ImportResult(int created, int skipped) {}

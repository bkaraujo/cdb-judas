package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A preview/confirm row: an expanded {@link TransactionDraft} plus preview-only fields. {@code
 * duplicate} flags a draft already present for the matched card; {@code categoryId} is filled by a
 * later slice (always {@code null} here).
 */
@NullMarked
public record PreviewRow(
        TransactionDraft draft,
        boolean duplicate,
        @Nullable UUID categoryId) {}

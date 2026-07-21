package br.cdb.feature.f006._1_application.preview;

import br.cdb.feature.f006._0_domain.TransactionDraft;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A preview/confirm row: an expanded {@link TransactionDraft} plus preview-only fields. {@code
 * duplicate} flags a draft already present for the suggested card; {@code categoryId} is the guessed
 * (or fallback) category. {@code suggestedCardId} is the card whose {@code last4} uniquely matched this
 * charge — the per-row pre-selection the user can override on confirm; {@code null} when the charge's
 * last4 matched no card or several.
 */
@NullMarked
public record PreviewRow(
        TransactionDraft draft,
        boolean duplicate,
        @Nullable UUID categoryId,
        @Nullable UUID suggestedCardId
) {}

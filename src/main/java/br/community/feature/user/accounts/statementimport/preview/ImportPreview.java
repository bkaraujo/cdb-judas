package br.community.feature.user.accounts.statementimport.preview;

import br.community.context.monetary._0_domain.model.MonetaryAccount;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Credit-card statement import preview: the detected issuer, the parsed statement (last4s + kept
 * charge rows), the matched registered card (null when none or ambiguous), the full list of
 * registered credit cards offered as manual-pick candidates and the expanded preview rows (one per
 * à-vista charge, {@code N} per parcelado charge, each with a duplicate flag). Grows with later
 * slices (guessed category).
 */
@NullMarked
public record ImportPreview(
        Issuer issuer,
        ParsedStatement statement,
        @Nullable MonetaryAccount matchedCard,
        List<MonetaryAccount> candidateCards,
        List<PreviewRow> rows) {}

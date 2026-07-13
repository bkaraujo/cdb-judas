package br.cdb.feature.user.accounts.transactions.importer.preview;

import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.feature.user.accounts.statement.Issuer;
import br.cdb.feature.user.accounts.statement.MonetaryDocumentEntry;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Credit-card statement import preview: the detected issuer, the parsed statement (last4s + kept
 * charge rows), the registered credit cards present on the statement (offered for per-row pick) and
 * the expanded preview rows (one per à-vista charge, {@code N} per parcelado charge, each with a
 * duplicate flag and a per-row suggested card).
 */
@NullMarked
public record ImportPreview(
        Issuer issuer,
        List<MonetaryDocumentEntry> statement,
        List<CreditCard> candidateCards,
        List<PreviewRow> rows) {

    /**
     * Distinct last4s across the kept charge lines. A statement can carry charges from several cards
     * (titular + additional cardholders), so the cards are derived from the lines rather than assumed
     * to be a single one.
     */
    public List<String> last4s() {
        return statement.stream().map(MonetaryDocumentEntry::last4).distinct().toList();
    }
}

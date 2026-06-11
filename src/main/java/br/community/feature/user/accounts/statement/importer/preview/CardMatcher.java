package br.community.feature.user.accounts.statement.importer.preview;

import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches a statement's distinct card last4s against the registered credit cards by their
 * {@code additionalInfo().get("last4")}. A statement may carry several cards (titular + additional
 * cardholders), so every last4 is considered: 1 matching card → {@link CardMatch.Matched}, 0 →
 * {@link CardMatch.NoMatch}, ≥2 → {@link CardMatch.Ambiguous}. Cards without a last4 are skipped.
 */
@NullMarked
public class CardMatcher {

    public CardMatch match(Collection<String> last4s, List<MonetaryAccount> cards) {
        val matching = cards.stream()
                .filter(card -> last4s
                        .contains((String) card.additionalInfo().getOrDefault("last4", Strings.EMPTY))
                )
                .toList();

        return switch (matching.size()) {
            case 0 -> new CardMatch.NoMatch();
            case 1 -> new CardMatch.Matched(matching.getFirst());
            default -> new CardMatch.Ambiguous(matching);
        };
    }

    /**
     * Resolves each distinct last4 to its registered card individually, so a statement carrying
     * charges from several cards can pre-select the right card per charge. An entry is produced only
     * when exactly one registered card carries that last4 — a last4 matched by zero or several cards is
     * left absent (the user picks manually). Cards without a last4 are skipped.
     */
    public Map<String, MonetaryAccount> matchByLast4(Collection<String> last4s, List<MonetaryAccount> cards) {
        val byLast4 = new HashMap<String, MonetaryAccount>();
        for (val last4 : last4s) {
            val matching = cards.stream()
                    .filter(card -> last4.equals(card.additionalInfo().getOrDefault("last4", Strings.EMPTY)))
                    .toList();
            if (matching.size() == 1) {
                byLast4.put(last4, matching.getFirst());
            }
        }
        return byLast4;
    }
}

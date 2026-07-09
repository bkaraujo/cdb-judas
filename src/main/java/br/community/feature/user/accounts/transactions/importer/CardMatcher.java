package br.community.feature.user.accounts.transactions.importer;

import br.community.context.monetary._0_domain.model.CreditCard;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches a statement's distinct card last4s against the registered credit cards by their
 * {@link CreditCard#last4()}. A statement may carry several cards (titular + additional cardholders),
 * so every last4 is considered: 1 matching card → {@link CardMatch.Matched}, 0 →
 * {@link CardMatch.NoMatch}, ≥2 → {@link CardMatch.Ambiguous}.
 */
@NullMarked
public class CardMatcher {

    public CardMatch match(Collection<String> last4s, List<CreditCard> cards) {
        val matching = cards.stream()
                .filter(card -> last4s.contains(card.last4()))
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
     * left absent (the user picks manually).
     */
    public Map<String, CreditCard> matchByLast4(Collection<String> last4s, List<CreditCard> cards) {
        val byLast4 = new HashMap<String, CreditCard>();
        for (val last4 : last4s) {
            val matching = cards.stream()
                    .filter(card -> last4.equals(card.last4()))
                    .toList();
            if (matching.size() == 1) {
                byLast4.put(last4, matching.getFirst());
            }
        }
        return byLast4;
    }
}

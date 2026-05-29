package br.community.feature.user.accounts.statementimport;

import br.community.context.monetary._0_domain.model.MonetaryAccount;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Matches a statement's distinct card last4s against the registered credit cards by their
 * {@code additionalInfo().get("last4")}. A statement may carry several cards (titular + additional
 * cardholders), so every last4 is considered: 1 matching card → {@link CardMatch.Matched}, 0 →
 * {@link CardMatch.NoMatch}, ≥2 → {@link CardMatch.Ambiguous}. Cards without a last4 are skipped.
 */
@NullMarked
public class CardMatcher {

    public CardMatch match(List<String> statementLast4s, List<MonetaryAccount> registeredCards) {
        final List<MonetaryAccount> matching = registeredCards.stream()
                .filter(card -> {
                    final Object last4 = card.additionalInfo().get("last4");
                    return last4 != null && statementLast4s.contains(String.valueOf(last4));
                })
                .toList();

        return switch (matching.size()) {
            case 0 -> new CardMatch.NoMatch();
            case 1 -> new CardMatch.Matched(matching.getFirst());
            default -> new CardMatch.Ambiguous(matching);
        };
    }
}

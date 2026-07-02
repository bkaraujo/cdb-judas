package br.community.feature.user.accounts.cards;

import br.community.context.monetary._0_domain.model.Card;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record CardResponse(UUID id, String last4, UUID accountId, boolean active) {
    public static CardResponse from(Card card) {
        return new CardResponse(card.id(), card.last4(), card.accountId(), card.active());
    }
}

package br.cdb.feature.f003._1_application;

import br.cdb.context.monetary._0_domain.model.CreditCard;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record CardResponse(UUID id, String last4, UUID accountId, boolean active) {
    public static CardResponse from(CreditCard creditCard) {
        return new CardResponse(creditCard.id(), creditCard.last4(), creditCard.accountId(), creditCard.active());
    }
}

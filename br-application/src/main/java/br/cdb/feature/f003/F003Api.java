package br.cdb.feature.f003;

import br.cdb.core.View;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** Cliente da API pública de {@code f003} */
@NullMarked
public interface F003Api {

    /** Também é o retorno JSON de {@code AccountCardResource} — mesmo tipo dos dois lados. */
    @NullMarked
    @Schema(name = "CardResponse")
    record CardView(UUID id, String last4, UUID accountId, boolean active) implements View {
        public static CardView from(CreditCard creditCard) {
            return new CardView(creditCard.id(), creditCard.last4(), creditCard.accountId(), creditCard.active());
        }
    }

    /** Corpo de {@link #createCard} — espelha {@code CardRequest}. */
    @NullMarked
    record CardBody(String last4) {}

    /** Cartões da conta. */
    List<CardView> cards(UUID accountId);

    CardView createCard(UUID accountId, CardBody body);

    CardView setCardActive(UUID accountId, UUID cardId, boolean active);

    /** Contrato uniforme de exclusão (ver {@code f000.Deletions}): {@code strategy} nulo = exclusão
     *  simples; {@code MOVE} exige {@code targetId}. */
    void deleteCard(UUID accountId, UUID cardId, @Nullable String strategy, @Nullable UUID targetId);

}

package br.cdb.feature.f003;

import br.cdb.core.View;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f003._2_infrastructure.web.AccountCardResource;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Cliente tipado da própria API pública de {@code f003}, para consumo cross-slice: publicado pela
 * fatia dona do endpoint, no mesmo papel de {@code f002.F002Api}/{@code f004.F004Api}/
 * {@code f006.F006Api}.
 *
 * <p>HTTP real via {@link HTTPApi} contra as rotas públicas de {@link AccountCardResource}, com
 * o mesmo {@code AuthenticationFilter}/{@code OwnershipFilter} e token efêmero. O consumidor não
 * alcança serviço, repositório nem o modelo {@code CreditCard} de {@code f003}.
 *
 * <p>Espelha toda a interface pública da fatia (D2 de {@code .claude/plan.md}); {@link #cards} é o
 * método com consumidor hoje ({@code f007}, na importação de fatura — casamento de cartão e
 * resolução de conta por cartão).
 *
 * <p>Context-wired ({@code Context.tryGet(F003Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F003Api {

    /** Espelha {@code CardResponse}. */
    @NullMarked
    public record CardView(UUID id, String last4, UUID accountId, boolean active) implements View {}

    /** Corpo de {@link #createCard} — espelha {@code CardRequest}. */
    @NullMarked
    public record CardBody(String last4) {}

    private static HTTPApi internalApi() {
        return Context.get(HTTPApi.class);
    }

    /** Cartões da conta. */
    public List<CardView> cards(UUID accountId) {
        return List.of(internalApi().get("/accounts/" + accountId + "/cards", CardView[].class));
    }

    public CardView createCard(UUID accountId, CardBody body) {
        return internalApi().post("/accounts/" + accountId + "/cards", body, CardView.class);
    }

    public CardView setCardActive(UUID accountId, UUID cardId, boolean active) {
        return internalApi().patch("/accounts/" + accountId + "/cards/" + cardId, new CardStatusBody(active), CardView.class);
    }

    /** Corpo de {@link #setCardActive} — espelha {@code CardStatusRequest}. */
    @NullMarked
    private record CardStatusBody(boolean active) {}

    /** Contrato uniforme de exclusão (ver {@code f000.Deletions}): {@code strategy} nulo = exclusão
     *  simples; {@code MOVE} exige {@code targetId}. */
    public void deleteCard(UUID accountId, UUID cardId, @Nullable String strategy, @Nullable UUID targetId) {
        internalApi().delete("/accounts/" + accountId + "/cards/" + cardId + deletionQuery(strategy, targetId));
    }

    private static String deletionQuery(@Nullable String strategy, @Nullable UUID targetId) {
        if (strategy == null) return "";
        return targetId == null ? "?strategy=" + strategy : "?strategy=" + strategy + "&targetId=" + targetId;
    }
}

package br.cdb.feature.f003._2_infrastructure;

import br.cdb.core.web.AbstractApiClient;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f003.F003Api;
import br.cdb.feature.f003._1_application.usecase.ReadUseCase;
import br.cdb.feature.f003._2_infrastructure.web.AccountCardResource;
import br.commons.framework.cdi.Context;
import lombok.val;
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
 * <p>Context-wired ({@code Context.get(F003Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F003ApiImpl extends AbstractApiClient implements F003Api {

    @Override
    public List<CardView> cards(UUID accountId) {
        val reads = Context.tryGet(ReadUseCase.class);
        return unwrap(reads.cards(accountId), cards -> cards.stream().map(CardView::from).toList());
    }

    @Override
    public CardView createCard(UUID accountId, CardBody body) {
        return post("/accounts/" + accountId + "/cards", body, CardView.class);
    }

    @Override
    public CardView setCardActive(UUID accountId, UUID cardId, boolean active) {
        return patch("/accounts/" + accountId + "/cards/" + cardId, new CardStatusBody(active), CardView.class);
    }

    /** Corpo de {@link #setCardActive} — espelha {@code CardStatusRequest}. */
    @NullMarked
    private record CardStatusBody(boolean active) {}

    @Override
    public void deleteCard(UUID accountId, UUID cardId, @Nullable String strategy, @Nullable UUID targetId) {
        delete("/accounts/" + accountId + "/cards/" + cardId + deletionQuery(strategy, targetId));
    }

    private static String deletionQuery(@Nullable String strategy, @Nullable UUID targetId) {
        if (strategy == null) return "";
        return targetId == null ? "?strategy=" + strategy : "?strategy=" + strategy + "&targetId=" + targetId;
    }
}

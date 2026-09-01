package br.cdb.feature.f003._1_application.usecase;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.cache.CreditCardCache;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Toda a leitura de cartão da fatia {@code f003} — o par de {@link WriteUseCase}, mesmo arranjo CQRS
 * de {@code f002}/{@code f006}. Context-wired como as demais classes ex-contexto
 * ({@code Context.tryGet(ReadUseCase.class)}, nunca {@code @Inject}); o {@code AccountCardResource}
 * lê <b>só</b> daqui.
 *
 * <p>As duas camadas convivem no mesmo tipo: o método de <b>entrada</b> ({@link #cards}) aplica a
 * guarda de propriedade da conta ({@link UserGuards}, bean CDI resolvido <b>por chamada</b> —
 * {@code @RequestScoped}, nunca guardado em campo); os de <b>engine</b> ({@link #list}), consumidos
 * cross-slice por {@code f000.UserGuards}, {@code f006} (importação) e {@code f999} (dispatch SSE),
 * contam com a <b>guarda implícita</b> do {@code F003_CARD.COD_PERSON} no WHERE.
 *
 * <p>Nota: o nome simples coincide com o {@code ReadUseCase} de {@code f002} — quem precisa dos dois
 * referencia um deles pelo nome completo.
 */
@NullMarked
public class ReadUseCase {

    private final CreditCardCache cache = Context.tryGet(CreditCardCache.class);
    private final AccountService accountService = Context.tryGet(AccountService.class);
    private final ReadUseCases transactions = Context.tryGet(ReadUseCases.class);

    /** Bean CDI resolvido a cada chamada: {@code @RequestScoped}, nunca guardado em campo. */
    private static UserGuards guards() {
        return Context.get(UserGuards.class);
    }

    // ── Cartões (entrada HTTP) ─────────────────────────────────────

    public Result<List<CreditCard>, BusinessError> cards(UUID accountId) {
        return guards().ownsAccount(accountId).flatMap(ignored -> list(accountId, HTTPRequest.personId()));
    }

    // ── Cartões (engine — guarda implícita por COD_PERSON) ─────────

    public Result<List<CreditCard>, BusinessError> list(String personId) {
        return cache.list(personId)
                .mapFailure(error -> (BusinessError) new BusinessError.BusinessRule("core.cache.unavailable"))
                .map(set -> new ArrayList<>(set));
    }

    public Result<List<CreditCard>, BusinessError> list(UUID accountId, String personId) {
        return accountService.findByIdAndPerson(accountId, personId)
                .flatMap(ignored -> list(personId))
                .map(cards -> cards.stream().filter(c -> accountId.equals(c.accountId())).toList());
    }

    // ── Leituras auxiliares do lado de escrita ─────────────────────

    /** Quantas transações da pessoa estão ligadas ao cartão — usado pelo delete sem estratégia
     *  ({@code DeletionOutcome.Linked}). */
    public int transactionCount(String personId, UUID cardId) {
        return (int) transactions.transactions(personId).getOrElse(List.of()).stream()
                .filter(t -> cardId.equals(t.cardId()))
                .count();
    }
}

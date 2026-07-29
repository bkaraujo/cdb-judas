package br.cdb.feature.f999._1_application;

import br.cdb.feature.f002._1_application.usecase.AccountUseCase;
import br.cdb.feature.f003._1_application.usecase.CreditCardUseCase;
import br.cdb.feature.f006._1_application.usecase.TransactionUseCase;
import br.cdb.feature.f000._0_domain.SSE;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f002._1_application.AccountResponse;
import br.cdb.feature.f002._1_application.UserAccountService;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Único dono do dispatch SSE de conta — reage ao vocabulário {@link AccountStreamEvents}, publicado
 * pelas fatias (f002/f004/f005/f006) só depois da mutação já persistida (contexto + overlay).
 * {@code personId} vem sempre do evento, nunca de {@code HTTPRequest.personId()} — best-effort, nunca
 * propaga falha para quem publicou.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class AccountStreamListener {

    private static final String TYPE = "ACCOUNT";

    private final AccountUseCase ucAccount = Registry.tryGet(AccountUseCase.class);
    private final CreditCardUseCase ucCreditCard = Registry.tryGet(CreditCardUseCase.class);
    private final TransactionUseCase ucTransaction = Registry.tryGet(TransactionUseCase.class);

    private final SSE sse;
    private final UserAccountService userAccountService;

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onCreated(AccountStreamEvents.Created event) {
        dispatchUpsert(event.accountId(), event.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(AccountStreamEvents.Updated event) {
        dispatchUpsert(event.accountId(), event.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onRefresh(AccountStreamEvents.Refresh event) {
        dispatchUpsert(event.accountId(), event.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(AccountStreamEvents.Deleted event) {
        dispatchDelete(event.accountId(), event.personId());
        return MessageResult.CONSUMED;
    }

    @SuppressWarnings("EmptyCatch")
    private void dispatchUpsert(UUID accountId, String personId) {
        try {
            switch (ucAccount.findAccount(accountId, personId)) {
                case Result.Success(var account) -> {
                    val ua = userAccountService.find(personId, accountId);
                    val cards = ucCreditCard.list(accountId, personId).getOrElse(List.of());
                    val transactions = ucTransaction.transactions(personId).getOrElse(List.of());
                    val dto = AccountResponse.from(account, ua, cards, transactions);
                    sse.dispatch(personId, SSE.Event.UPSERT, Map.of("type", TYPE, "payload", dto));
                }
                case Result.Failure(var ignored) -> { }
            }
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void dispatchDelete(UUID accountId, String personId) {
        try {
            sse.dispatch(personId, SSE.Event.DELETE, Map.of("type", TYPE, "id", accountId.toString()));
        } catch (Exception ignored) {}
    }
}

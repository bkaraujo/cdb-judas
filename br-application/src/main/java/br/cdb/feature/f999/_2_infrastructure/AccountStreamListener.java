package br.cdb.feature.f999._2_infrastructure;

import br.cdb.core.web.InternalCall;
import br.cdb.feature.f000._0_domain.SSE;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f002.F002Api;
import br.cdb.feature.f002._0_domain.event.AccountEvents;
import br.cdb.feature.f003._0_domain.event.CreditCardEvents;
import br.commons.MessageBus;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.UUID;

/**
 * Único dono do dispatch SSE de conta — reage direto aos eventos CRUD que {@code f002} ({@link
 * AccountEvents}) e {@code f003} ({@link CreditCardEvents}) já publicam pós-mutação, mais o fan-in
 * residual de {@link AccountStreamEvents.Refresh} (contas afetadas indiretamente por f005/f006/f007
 * e pelo alvo do MOVE de conta — ver {@code f002.WriteUseCase#deleteAccount}). {@code personId} vem
 * sempre do evento, nunca de {@code HTTPRequest.personId()} — best-effort, nunca propaga falha para
 * quem publicou.
 *
 * <p>Payload é o mesmo {@code F002Api.AccountView} de {@code GET /accounts/{id}} — já traz saldo e
 * cartões calculados pela fatia dona ({@code f002}); este listener não recalcula nada nem toca
 * modelo/use case de fatia irmã (só via {@link F002Api}, sob {@link InternalCall#as}: o evento pode
 * chegar fora de uma requisição HTTP, como no job de reconciliação de {@code f999}).
 */
@NullMarked
@Singleton
public class AccountStreamListener {

    private static final String TYPE = "ACCOUNT";

    private final SSE sse = Context.get(SSE.class);

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onAccountCreated(AccountEvents.Created event) {
        dispatchUpsert(event.account().id(), event.account().personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onAccountUpdated(AccountEvents.Updated event) {
        dispatchUpsert(event.account().id(), event.account().personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onAccountDeleted(AccountEvents.Deleted event) {
        dispatchDelete(event.id(), event.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCardCreated(CreditCardEvents.Created event) {
        dispatchUpsert(event.creditCard().accountId(), event.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCardUpdated(CreditCardEvents.Updated event) {
        dispatchUpsert(event.creditCard().accountId(), event.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCardDeleted(CreditCardEvents.Deleted event) {
        dispatchUpsert(event.accountId(), event.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onRefresh(AccountStreamEvents.Refresh event) {
        dispatchUpsert(event.accountId(), event.personId());
        return MessageResult.CONSUMED;
    }

    @SuppressWarnings("EmptyCatch")
    private void dispatchUpsert(UUID accountId, String personId) {
        try {
            val account = InternalCall.as(personId, () -> Context.get(F002Api.class).account(accountId));
            sse.dispatch(personId, SSE.Event.UPSERT, Map.of("type", TYPE, "payload", account));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void dispatchDelete(UUID accountId, String personId) {
        try {
            sse.dispatch(personId, SSE.Event.DELETE, Map.of("type", TYPE, "id", accountId.toString()));
        } catch (Exception ignored) {}
    }
}

package br.cdb.feature.f003._1_application.cache;

import br.cdb.core.cache.SessionScopedCache;
import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f003._0_domain.event.CreditCardEvents;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@NullMarked
public class CreditCardCache {
    private final SessionScopedCache<CreditCard> store = new SessionScopedCache<>(
            CreditCardLayout.PREFIX,
            personId -> {
                val service = Context.tryGet(CreditCardService.class);
                return service != null ? service.findAllByPerson(personId) : List.of();
            },
            CreditCard::id,
            m -> CreditCardLayout.SIZE,
            CreditCardLayout::write);

    @MessageListener
    public MessageResult onLogin(SessionEvents.Login e) {
        store.onLogin(e.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onLogout(SessionEvents.Logout e) {
        store.onLogout(e.personId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCreated(CreditCardEvents.Created e) {
        val card = e.creditCard();
        val accountService = Context.tryGet(AccountService.class);
        val result = accountService.findById(card.accountId());
        if (result instanceof br.commons.Result.Success(var account)) {
            store.upsert(account.personId().toString(), card);
        }
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(CreditCardEvents.Updated e) {
        val card = e.creditCard();
        val accountService = Context.tryGet(AccountService.class);
        val result = accountService.findById(card.accountId());
        if (result instanceof br.commons.Result.Success(var account)) {
            store.upsert(account.personId().toString(), card);
        }
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(CreditCardEvents.Deleted e) {
        store.evictEverywhere(e.id());
        return MessageResult.CONSUMED;
    }

    public void forEach(UUID personId, Consumer<CreditCardLayout.View> consumer) {
        val view = new CreditCardLayout.View();
        store.forEach(personId.toString(), seg -> {
            consumer.accept(view.bind(seg));
        });
    }

    public boolean find(UUID personId, UUID id, Consumer<CreditCardLayout.View> consumer) {
        val view = new CreditCardLayout.View();
        return store.find(personId.toString(), id, seg -> {
            consumer.accept(view.bind(seg));
        });
    }
}

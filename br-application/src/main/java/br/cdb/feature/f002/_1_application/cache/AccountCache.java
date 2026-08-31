package br.cdb.feature.f002._1_application.cache;

import br.cdb.core.cache.SessionScopedCache;
import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f002._0_domain.event.AccountEvents;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._1_application.service.AccountService;
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
public class AccountCache {
    private final SessionScopedCache<Account> store = new SessionScopedCache<>(
            AccountLayout.PREFIX,
            personId -> {
                val service = Context.tryGet(AccountService.class);
                return service != null ? service.findAllByPerson(personId) : List.of();
            },
            Account::id,
            m -> AccountLayout.SIZE,
            AccountLayout::write);

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
    public MessageResult onCreated(AccountEvents.Created e) {
        store.upsert(e.account().personId(), e.account());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(AccountEvents.Updated e) {
        store.upsert(e.account().personId(), e.account());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(AccountEvents.Deleted e) {
        store.evictEverywhere(e.id());
        return MessageResult.CONSUMED;
    }

    public void forEach(UUID personId, Consumer<AccountLayout.View> consumer) {
        val view = new AccountLayout.View();
        store.forEach(personId.toString(), seg -> {
            consumer.accept(view.bind(seg));
        });
    }

    public boolean find(UUID personId, UUID id, Consumer<AccountLayout.View> consumer) {
        val view = new AccountLayout.View();
        return store.find(personId.toString(), id, seg -> {
            consumer.accept(view.bind(seg));
        });
    }
}

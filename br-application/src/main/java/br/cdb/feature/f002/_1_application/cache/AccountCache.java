package br.cdb.feature.f002._1_application.cache;

import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f002._0_domain.event.AccountEvents;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.commons.cache.AbstractCache;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

@NullMarked
public class AccountCache extends AbstractCache<Account> {

    public AccountCache() {
        super(
                AccountLayout.PREFIX,
                personId -> {
                    val service = Context.tryGet(AccountService.class);
                    return service.findAllByPerson(personId);
                },
                Account::id,
                _ -> AccountLayout.SIZE,
                AccountLayout::write
        );
    }

    @Override
    protected Account mapToDomain(MemorySegment segment) {
        val view = new AccountLayout.View().bind(segment);
        return new Account(
                view.id(), view.name(), view.type(), view.active(),
                view.personId(), view.color(), view.creditLimit(),
                view.overdraftLimit(), view.closingDay(), view.dueDay(),
                view.createdAt(), view.updatedAt()
        );
    }

    @MessageListener
    public MessageResult onLogin(SessionEvents.Login e) {
        onLogin(e.personId().toString());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onLogout(SessionEvents.Logout e) {
        onLogout(e.personId().toString());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCreated(AccountEvents.Created e) {
        upsert(e.account().personId(), e.account());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(AccountEvents.Updated e) {
        upsert(e.account().personId(), e.account());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(AccountEvents.Deleted e) {
        evict(e.personId(), e.id());
        return MessageResult.CONSUMED;
    }
}

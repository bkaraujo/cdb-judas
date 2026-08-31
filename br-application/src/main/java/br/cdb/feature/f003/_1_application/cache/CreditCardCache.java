package br.cdb.feature.f003._1_application.cache;

import br.cdb.core.security.SessionEvents;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f003._0_domain.event.CreditCardEvents;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.commons.Result;
import br.commons.cache.AbstractCache;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

@NullMarked
public class CreditCardCache extends AbstractCache<CreditCard> {

    public CreditCardCache() {
        super(
                CreditCardLayout.PREFIX,
                personId -> {
                    val service = Context.tryGet(CreditCardService.class);
                    return service.findAllByPerson(personId);
                },
                CreditCard::id,
                    _ -> CreditCardLayout.SIZE,
                CreditCardLayout::write
        );
    }

    @Override
    protected CreditCard mapToDomain(MemorySegment segment) {
        val view = new CreditCardLayout.View().bind(segment);
        return new CreditCard(
                view.id(), view.last4(), view.accountId(), view.active(),
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
    public MessageResult onCreated(CreditCardEvents.Created e) {
        val card = e.creditCard();
        val accountService = Context.tryGet(AccountService.class);
        val result = accountService.findById(card.accountId());
        if (result instanceof Result.Success(var account)) {
            upsert(account.personId().toString(), card);
        }
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(CreditCardEvents.Updated e) {
        val card = e.creditCard();
        val accountService = Context.tryGet(AccountService.class);
        val result = accountService.findById(card.accountId());
        if (result instanceof Result.Success(var account)) {
            upsert(account.personId().toString(), card);
        }
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(CreditCardEvents.Deleted e) {
        evictEverywhere(e.id());
        return MessageResult.CONSUMED;
    }
}

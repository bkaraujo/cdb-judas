package br.community.context.monetary._1_application.event;

import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.monetary._0_domain.event.MonetaryEvent;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._1_application.AccountView;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.shared._1_application.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;

@NullMarked
@RequiredArgsConstructor
public class AccountEventListener {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @MessageListener
    public MessageResult onAccountUpdated(MonetaryEvent.AccountUpdated event) {
        val account = event.account();

        for (val card : accountService.findCreditCardsByAccount(account.id())) {
            if (!card.color().equalsIgnoreCase(account.color())) {
                val updated = new MonetaryAccount(
                        card.id(), card.name(), card.type(), card.balance(),
                        account.color(), card.active(), card.linkedAccountId()
                );
                updated.additionalInfo().putAll(card.additionalInfo());
                val saved = accountService.save(updated);
                DomainEventPublisher.upsert("ACCOUNT", withCurrentBalance(saved));
            }
        }

        return MessageResult.AVAILABLE;
    }

    private AccountView withCurrentBalance(MonetaryAccount account) {
        val transactions = transactionService.findByAccount(account.id());
        var sum = BigDecimal.ZERO;
        for (val t : transactions) sum = sum.add(t.amount());
        return AccountView.of(account, account.balance().add(sum));
    }
}

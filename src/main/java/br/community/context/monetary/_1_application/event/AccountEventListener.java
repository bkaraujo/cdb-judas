package br.community.context.monetary._1_application.event;

import br.commons.MessageBus;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.monetary._0_domain.event.AccountEvents;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._1_application.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@NullMarked
@RequiredArgsConstructor
public class AccountEventListener {

    private final AccountService accountService;

    /**
     * Propaga a cor da conta para os cartões de crédito vinculados. Cada cartão alterado re-emite
     * {@link AccountEvents.Updated} — a camada de feature converte esse evento em SSE.
     * Retorna {@code CONSUMED} para não interromper a cadeia: outros assinantes (inclusive o stream
     * da feature) também precisam receber o evento. A recursão termina porque um cartão não possui
     * cartões vinculados.
     */
    @MessageListener
    public MessageResult onAccountUpdated(AccountEvents.Updated event) {
        val account = event.account();

        for (val card : accountService.findCreditCardsByAccount(account.id())) {
            if (!card.color().equalsIgnoreCase(account.color())) {
                val updated = new Account(
                        card.id(), card.name(), card.type(), card.balance(),
                        account.color(), card.active(), card.linkedAccountId()
                );
                updated.additionalInfo().putAll(card.additionalInfo());
                val saved = accountService.save(updated);
                MessageBus.submit(new AccountEvents.Updated(saved));
            }
        }

        return MessageResult.CONSUMED;
    }
}

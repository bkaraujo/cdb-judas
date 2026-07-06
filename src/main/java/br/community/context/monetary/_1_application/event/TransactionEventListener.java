package br.community.context.monetary._1_application.event;

import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.monetary._0_domain.event.TransactionEvents;
import br.community.context.monetary._1_application.service.BalanceRecalculationService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

@RequiredArgsConstructor
@NullMarked
public class TransactionEventListener {

    private final BalanceRecalculationService balanceRecalculationService;

    @MessageListener
    public MessageResult onTransaction(TransactionEvents.Created transaction) {
        balanceRecalculationService.recalculate(transaction.transaction().accountId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTransaction(TransactionEvents.Updated transaction) {
        balanceRecalculationService.recalculate(transaction.transaction().accountId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTransaction(TransactionEvents.Deleted transaction) {
        balanceRecalculationService.recalculate(transaction.transaction().accountId());
        return MessageResult.CONSUMED;
    }
}

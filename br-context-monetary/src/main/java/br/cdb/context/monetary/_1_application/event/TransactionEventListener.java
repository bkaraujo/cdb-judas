package br.cdb.context.monetary._1_application.event;

import br.cdb.context.monetary._0_domain.event.TransactionEvents;
import br.cdb.context.monetary._1_application.service.BalanceRecalculationService;
import br.commons.Registry;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TransactionEventListener {

    private final BalanceRecalculationService balanceRecalculationService = Registry.tryGet(BalanceRecalculationService.class);

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

package br.cdb.context.monetary._1_application.event;

import br.cdb.context.monetary._0_domain.event.TransactionEvents;
import br.cdb.context.monetary._1_application.service.BalanceService;
import br.commons.Registry;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TransactionEventListener {

    private final BalanceService service = Registry.tryGet(BalanceService.class);

    @MessageListener
    public MessageResult onTransaction(TransactionEvents.Created transaction) {
        service.recalculate(transaction.transaction().accountId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTransaction(TransactionEvents.Updated transaction) {
        service.recalculate(transaction.transaction().accountId());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTransaction(TransactionEvents.Deleted transaction) {
        service.recalculate(transaction.transaction().accountId());
        return MessageResult.CONSUMED;
    }
}

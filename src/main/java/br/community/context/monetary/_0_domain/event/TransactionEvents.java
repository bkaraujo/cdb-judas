package br.community.context.monetary._0_domain.event;

import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.shared._0_domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TransactionEvents extends DomainEvent {

    @NullMarked
    record Created(Transaction transaction) implements TransactionEvents {}

    @NullMarked
    record Updated(Transaction transaction) implements TransactionEvents {}

    @NullMarked
    record Deleted(Transaction transaction) implements TransactionEvents {}
}

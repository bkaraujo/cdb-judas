package br.cdb.feature.f006._0_domain.event;

import br.cdb.feature.f006._0_domain.model.Transaction;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TransactionEvents extends BusinessEvent {

    @NullMarked
    record Created(Transaction transaction) implements TransactionEvents {}

    @NullMarked
    record Updated(Transaction transaction) implements TransactionEvents {}

    @NullMarked
    record Deleted(Transaction transaction) implements TransactionEvents {}
}

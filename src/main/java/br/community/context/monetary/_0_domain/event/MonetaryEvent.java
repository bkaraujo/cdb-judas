package br.community.context.monetary._0_domain.event;

import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.shared._0_domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface MonetaryEvent extends DomainEvent {

    @NullMarked
    record AccountCreated(MonetaryAccount account) implements DomainEvent{}

    @NullMarked
    record AccountUpdated(MonetaryAccount account) implements DomainEvent{}

    @NullMarked
    record AccountDeleted(UUID accountId) implements DomainEvent{}

    @NullMarked
    record TransactionCreated(MonetaryTransaction transaction) implements DomainEvent{}

    @NullMarked
    record TransactionDeleted(MonetaryTransaction transaction) implements DomainEvent{}

    @NullMarked
    record TransactionUpdated(MonetaryTransaction transaction) implements DomainEvent{}

}

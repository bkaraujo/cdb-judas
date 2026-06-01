package br.community.context.monetary._0_domain.event;

import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.model.MonetaryCategory;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._0_domain.model.Tag;
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

    @NullMarked
    record CategoryCreated(MonetaryCategory category) implements DomainEvent{}

    @NullMarked
    record CategoryUpdated(MonetaryCategory category) implements DomainEvent{}

    @NullMarked
    record CategoryDeleted(UUID categoryId) implements DomainEvent{}

    @NullMarked
    record TagCreated(Tag tag) implements DomainEvent{}

    @NullMarked
    record TagUpdated(Tag tag) implements DomainEvent{}

    @NullMarked
    record TagDeleted(UUID tagId) implements DomainEvent{}

}

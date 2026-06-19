package br.community.context.monetary._0_domain.event;

import br.community.context.monetary._0_domain.model.Account;
import br.community.context.shared._0_domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface AccountEvents extends DomainEvent {

    @NullMarked
    record Created(Account account) implements AccountEvents {}

    @NullMarked
    record Updated(Account account) implements AccountEvents {}

    @NullMarked
    record Deleted(UUID accountId) implements AccountEvents {}
}

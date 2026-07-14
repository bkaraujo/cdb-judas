package br.cdb.context.monetary._0_domain.event;

import br.cdb.context.monetary._0_domain.model.Account;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface AccountEvents extends BusinessEvent {

    @NullMarked
    record Created(Account account) implements AccountEvents {}

    @NullMarked
    record Updated(Account account) implements AccountEvents {}

    @NullMarked
    record Deleted(UUID id) implements AccountEvents {}
}

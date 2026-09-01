package br.cdb.feature.f002._0_domain.event;

import br.cdb.feature.f002._0_domain.model.Account;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface AccountEvents extends BusinessEvent {

    @NullMarked
    record Created(Account account) implements AccountEvents {}

    @NullMarked
    record Updated(Account account) implements AccountEvents {}

    /** {@code personId} viaja aqui porque a linha já foi apagada quando o consumidor (SSE de f999)
     *  reage — não há mais o que consultar. */
    @NullMarked
    record Deleted(UUID id, String personId) implements AccountEvents {}
}

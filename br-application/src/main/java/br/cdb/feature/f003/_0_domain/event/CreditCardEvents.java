package br.cdb.feature.f003._0_domain.event;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface CreditCardEvents extends BusinessEvent {

    @NullMarked
    record Created(CreditCard creditCard) implements CreditCardEvents {}

    @NullMarked
    record Updated(CreditCard creditCard) implements CreditCardEvents {}

    @NullMarked
    record Deleted(UUID id) implements CreditCardEvents {}
}

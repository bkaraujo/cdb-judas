package br.cdb.feature.f003._0_domain.event;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * {@code personId} viaja em todos os três porque {@link CreditCard} não tem esse campo (só
 * {@code accountId}) — o publicador resolve o dono uma vez via {@code AccountService.findById},
 * poupando os consumidores (cache, SSE) de repetir a mesma busca.
 */
@NullMarked
public interface CreditCardEvents extends BusinessEvent {

    @NullMarked
    record Created(CreditCard creditCard, String personId) implements CreditCardEvents {}

    @NullMarked
    record Updated(CreditCard creditCard, String personId) implements CreditCardEvents {}

    /** {@code accountId} viaja aqui porque a linha já foi apagada quando o consumidor reage — não há
     *  mais {@code CreditCard} para consultar. */
    @NullMarked
    record Deleted(UUID id, UUID accountId, String personId) implements CreditCardEvents {}
}

package br.cdb.feature.f003._0_domain.repository;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public interface CreditCardRepository extends Repository<CreditCard, UUID> {
    /** Guarda implícita: só os cartões de {@code personId} — F003_CARD.COD_PERSON no WHERE. */
    List<CreditCard> findAllByPerson(String personId);

    /** Guarda implícita: só os cartões de {@code accountId} que pertencem a {@code personId}. */
    List<CreditCard> findByAccountAndPerson(UUID accountId, String personId);
}

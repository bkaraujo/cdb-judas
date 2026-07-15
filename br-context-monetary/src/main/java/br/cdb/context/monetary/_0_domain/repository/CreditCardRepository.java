package br.cdb.context.monetary._0_domain.repository;

import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface CreditCardRepository extends Repository<CreditCard, UUID> {
}

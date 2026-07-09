package br.community.context.monetary._0_domain.repository;

import br.commons.framework.persistence.json.Repository;
import br.community.context.monetary._0_domain.model.CreditCard;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface CardRepository extends Repository<CreditCard, UUID> {
}

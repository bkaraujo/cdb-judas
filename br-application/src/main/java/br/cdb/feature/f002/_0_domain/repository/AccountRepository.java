package br.cdb.feature.f002._0_domain.repository;

import br.cdb.feature.f002._0_domain.model.Account;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface AccountRepository extends Repository<Account, UUID> {
}

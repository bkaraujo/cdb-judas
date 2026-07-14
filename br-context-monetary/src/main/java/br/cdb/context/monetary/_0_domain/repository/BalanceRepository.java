package br.cdb.context.monetary._0_domain.repository;

import br.cdb.context.monetary._0_domain.model.AccountBalance;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@NullMarked
public interface BalanceRepository extends Repository<AccountBalance, UUID> {

    List<AccountBalance> findByAccount(UUID accountId);

    void delete(UUID uuid, YearMonth period);

}

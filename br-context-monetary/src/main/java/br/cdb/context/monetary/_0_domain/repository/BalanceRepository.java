package br.cdb.context.monetary._0_domain.repository;

import br.cdb.context.monetary._0_domain.model.MonthlyBalance;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public interface BalanceRepository extends Repository<MonthlyBalance, UUID> {

    List<MonthlyBalance> findByAccount(UUID accountId);

}

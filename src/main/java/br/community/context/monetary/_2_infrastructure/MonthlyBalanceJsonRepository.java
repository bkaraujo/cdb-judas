package br.community.context.monetary._2_infrastructure;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.AbstractJsonRepository;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import tools.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public final class MonthlyBalanceJsonRepository extends AbstractJsonRepository<MonthlyBalance, UUID> implements BalanceRepository {

    public MonthlyBalanceJsonRepository(ObjectMapper mapper, Storage storage) {
        super(mapper, storage, MonthlyBalance.class);
    }

    @Override
    protected String jsonKey() { return "monthlyBalances"; }

    @Override
    protected UUID getId(MonthlyBalance entity) { return entity.id(); }

    @Override
    public List<MonthlyBalance> findByAccount(UUID accountId) {
        return findAll().stream()
                .filter(b -> b.accountId().equals(accountId))
                .toList();
    }
}

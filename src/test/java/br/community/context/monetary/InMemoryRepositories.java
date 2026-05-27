package br.community.context.monetary;

import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.model.MonetaryCategory;
import br.community.context.monetary._0_domain.model.MonetaryCenter;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.model.Tag;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import br.community.context.monetary._0_domain.repository.CategoryRepository;
import br.community.context.monetary._0_domain.repository.ClosingRepository;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import br.community.context.monetary._0_domain.repository.TagRepository;
import br.community.context.monetary._0_domain.repository.TransactionRepository;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("MissingOverride")
final class InMemoryRepositories {
    private InMemoryRepositories() {}

    static abstract class BaseRepo<T, ID> {
        protected final Map<ID, T> data = new LinkedHashMap<>();
        public List<T> findAll() { return new ArrayList<>(data.values()); }
        public Optional<T> findById(ID id) { return Optional.ofNullable(data.get(id)); }
        public void deleteById(ID id) { data.remove(id); }
        public void clearCache() {}
    }

    static class Accounts extends BaseRepo<MonetaryAccount, UUID> implements AccountRepository {
        public MonetaryAccount save(MonetaryAccount e) { data.put(e.id(), e); return e; }
    }

    static class Transactions extends BaseRepo<MonetaryTransaction, UUID> implements TransactionRepository {
        public MonetaryTransaction save(MonetaryTransaction e) { data.put(e.id(), e); return e; }
    }

    static class Categories extends BaseRepo<MonetaryCategory, UUID> implements CategoryRepository {
        public MonetaryCategory save(MonetaryCategory e) { data.put(e.id(), e); return e; }
    }

    static class Tags extends BaseRepo<Tag, UUID> implements TagRepository {
        public Tag save(Tag e) { data.put(e.id(), e); return e; }
    }

    static class CostCenters extends BaseRepo<MonetaryCenter, UUID> implements CostCenterRepository {
        public MonetaryCenter save(MonetaryCenter e) { data.put(e.id(), e); return e; }
    }

    static class Balances extends BaseRepo<MonthlyBalance, UUID> implements BalanceRepository {
        public MonthlyBalance save(MonthlyBalance e) { data.put(e.id(), e); return e; }
        public List<MonthlyBalance> findByAccount(UUID accountId) {
            return data.values().stream().filter(b -> b.accountId().equals(accountId)).toList();
        }
    }

    static class Closings implements ClosingRepository {
        private YearMonth ym;
        public Optional<YearMonth> find() { return Optional.ofNullable(ym); }
        public void save(YearMonth ym) { this.ym = ym; }
        public void clear() { this.ym = null; }
    }
}

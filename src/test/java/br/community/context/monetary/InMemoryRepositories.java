package br.community.context.monetary;

import br.community.context.monetary._0_domain.model.*;
import br.community.context.monetary._0_domain.repository.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.util.*;

@NullMarked
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
        private @Nullable YearMonth ym;
        public Optional<YearMonth> find() { return Optional.ofNullable(ym); }
        public void save(YearMonth ym) { this.ym = ym; }
        public void clear() { this.ym = null; }
    }
}

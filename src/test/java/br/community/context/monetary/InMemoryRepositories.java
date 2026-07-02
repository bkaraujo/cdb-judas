package br.community.context.monetary;

import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.AccountLimit;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._0_domain.repository.AccountLimitRepository;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import br.community.context.monetary._0_domain.repository.CardRepository;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.feature.user.accounts.closing.ClosingRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
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

    static class Accounts extends BaseRepo<Account, UUID> implements AccountRepository {
        public Account save(Account e) { data.put(e.id(), e); return e; }
    }

    static class Transactions extends BaseRepo<Transaction, UUID> implements TransactionRepository {
        public Transaction save(Transaction e) { data.put(e.id(), e); return e; }
    }

    static class CostCenters extends BaseRepo<CostCenter, UUID> implements CostCenterRepository {
        public CostCenter save(CostCenter e) { data.put(e.id(), e); return e; }
    }

    static class Balances extends BaseRepo<MonthlyBalance, UUID> implements BalanceRepository {
        public MonthlyBalance save(MonthlyBalance e) { data.put(e.id(), e); return e; }
        public List<MonthlyBalance> findByAccount(UUID accountId) {
            return data.values().stream().filter(b -> b.accountId().equals(accountId)).toList();
        }
        public BigDecimal findOpeningBalance(UUID accountId) { return BigDecimal.ZERO; }
    }

    static class Cards extends BaseRepo<Card, UUID> implements CardRepository {
        public Card save(Card e) { data.put(e.id(), e); return e; }
    }

    static class AccountLimits extends BaseRepo<AccountLimit, UUID> implements AccountLimitRepository {
        public AccountLimit save(AccountLimit e) { data.put(e.accountId(), e); return e; }
    }

    static class Closings implements ClosingRepository {
        private @Nullable YearMonth ym;
        public Optional<YearMonth> find() { return Optional.ofNullable(ym); }
        public void save(YearMonth ym) { this.ym = ym; }
        public void clear() { this.ym = null; }
    }
}

package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.*;
import br.cdb.context.monetary._0_domain.repository.*;
import br.cdb.feature.user.accounts.closing.ClosingRepository;
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

    static class Accounts extends BaseRepo<Account, UUID> implements AccountRepository {
        public Account save(Account e) { data.put(e.id(), e); return e; }
    }

    static class Transactions extends BaseRepo<Transaction, UUID> implements TransactionRepository {
        public Transaction save(Transaction e) { data.put(e.id(), e); return e; }

        public void reassignAccount(UUID from, UUID to) {
            for (var t : List.copyOf(data.values())) {
                if (t.accountId().equals(from)) {
                    data.put(t.id(), new Transaction(t.id(), t.description(), t.signal(), t.amount(), t.purchasedAt(),
                            to, t.status(), t.costCenterId(), t.paymentDate(), t.groupId(),
                            t.installmentNumber(), t.totalInstallments(), t.notes(), t.createdAt(), t.updatedAt(), t.cardId()));
                }
            }
        }

        public void reassignCard(UUID from, UUID to) {
            for (var t : List.copyOf(data.values())) {
                if (from.equals(t.cardId())) {
                    data.put(t.id(), new Transaction(t.id(), t.description(), t.signal(), t.amount(), t.purchasedAt(),
                            t.accountId(), t.status(), t.costCenterId(), t.paymentDate(), t.groupId(),
                            t.installmentNumber(), t.totalInstallments(), t.notes(), t.createdAt(), t.updatedAt(), to));
                }
            }
        }
    }

    static class CostCenters extends BaseRepo<CostCenter, UUID> implements CostCenterRepository {
        public CostCenter save(CostCenter e) { data.put(e.id(), e); return e; }
    }

    static class Balances extends BaseRepo<MonthlyBalance, UUID> implements BalanceRepository {
        public MonthlyBalance save(MonthlyBalance e) { data.put(e.id(), e); return e; }
        public List<MonthlyBalance> findByAccount(UUID accountId) {
            return data.values().stream().filter(b -> b.accountId().equals(accountId)).toList();
        }
    }

    static class Cards extends BaseRepo<CreditCard, UUID> implements CardRepository {
        public CreditCard save(CreditCard e) { data.put(e.id(), e); return e; }
    }

    static class Closings implements ClosingRepository {
        private @Nullable YearMonth ym;
        public Optional<YearMonth> find() { return Optional.ofNullable(ym); }
        public void save(YearMonth ym) { this.ym = ym; }
        public void clear() { this.ym = null; }
    }
}

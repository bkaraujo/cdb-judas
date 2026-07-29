package br.cdb.context.monetary;

import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f000._0_domain.repository.CostCenterRepository;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.cdb.feature.f000._0_domain.ClosingRepository;
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
        public void clearCache() { data.clear(); }
    }

    static class Accounts extends BaseRepo<Account, UUID> implements AccountRepository {
        public Account save(Account e) { data.put(e.id(), e); return e; }

        /** Fake não modela COD_PERSON — testes de engine não cobrem guarda implícita (isso é BaseHttpTest). */
        public List<Account> findAllByPerson(String personId) { return findAll(); }
        public Optional<Account> findByIdAndPerson(UUID id, String personId) { return findById(id); }
    }

    static class Transactions extends BaseRepo<Transaction, UUID> implements TransactionRepository {
        public Transaction save(Transaction e) { data.put(e.id(), e); return e; }

        /** Fake não modela COD_PERSON — testes de engine não cobrem guarda implícita (isso é BaseHttpTest). */
        public List<Transaction> findAllByPerson(String personId) { return findAll(); }
        public Optional<Transaction> findByIdAndPerson(UUID id, String personId) { return findById(id); }

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

    /** Balance tem chave de negócio (accountId, period) — sem personId próprio, não cabe em BaseRepo. */
    static class Balances implements BalanceRepository {
        private final List<Balance> data = new ArrayList<>();

        public List<Balance> findAll() { return new ArrayList<>(data); }

        /** Sem personId próprio: nada aqui indexa por UUID solto — ver {@link #delete}. */
        public Optional<Balance> findById(UUID id) { return Optional.empty(); }

        public Balance save(Balance e) {
            data.removeIf(b -> b.account().id().equals(e.account().id()) && b.period().equals(e.period()));
            data.add(e);
            return e;
        }

        public void deleteById(UUID id) { /* sem personId próprio; ver delete(accountId, period) */ }

        public void delete(UUID accountId, YearMonth period) {
            data.removeIf(b -> b.account().id().equals(accountId) && b.period().equals(period));
        }

        public List<Balance> findByAccount(UUID accountId) {
            return data.stream().filter(b -> b.account().id().equals(accountId)).toList();
        }

        public void clearCache() { data.clear(); }
    }

    static class Cards extends BaseRepo<CreditCard, UUID> implements CreditCardRepository {
        public CreditCard save(CreditCard e) { data.put(e.id(), e); return e; }

        /** Fake não modela COD_PERSON — testes de engine não cobrem guarda implícita (isso é BaseHttpTest). */
        public List<CreditCard> findAllByPerson(String personId) { return findAll(); }
        public List<CreditCard> findByAccountAndPerson(UUID accountId, String personId) {
            return findAll().stream().filter(c -> accountId.equals(c.accountId())).toList();
        }
    }

    static class Closings implements ClosingRepository {
        private @Nullable YearMonth ym;
        public Optional<YearMonth> find() { return Optional.ofNullable(ym); }
        public void save(YearMonth ym) { this.ym = ym; }
        public void clear() { this.ym = null; }
    }
}

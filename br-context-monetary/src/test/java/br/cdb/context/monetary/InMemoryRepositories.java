package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.*;
import br.cdb.context.monetary._0_domain.repository.*;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.*;

/** Fakes em memória das portas de repositório — usados via {@link br.commons.Registry}, não injeção direta. */
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

    /** Balance tem chave de negócio (accountId, period) — sem id próprio, não cabe em BaseRepo. */
    static class Balances implements BalanceRepository {
        private final List<Balance> data = new ArrayList<>();

        public List<Balance> findAll() { return new ArrayList<>(data); }

        /** Sem id próprio: nada aqui indexa por UUID solto — ver {@link #delete}. */
        public Optional<Balance> findById(UUID id) { return Optional.empty(); }

        public Balance save(Balance e) {
            data.removeIf(b -> b.account().id().equals(e.account().id()) && b.period().equals(e.period()));
            data.add(e);
            return e;
        }

        public void deleteById(UUID id) { /* sem id próprio; ver delete(accountId, period) */ }

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
    }
}

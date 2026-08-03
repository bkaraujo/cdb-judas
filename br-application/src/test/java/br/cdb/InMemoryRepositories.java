package br.cdb;

import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._0_domain.repository.CostCenterRepository;
import br.cdb.feature.f000._0_domain.repository.PersonRepository;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f002._0_domain.repository.ClosingRepository;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.cdb.feature.f004._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f005._0_domain.Category;
import br.cdb.feature.f005._0_domain.CategoryRepository;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.util.*;

@NullMarked
@SuppressWarnings("MissingOverride")
public abstract class InMemoryRepositories {
    private InMemoryRepositories() {}

    static abstract class BaseRepo<T, ID> {
        protected final Map<ID, T> data = new LinkedHashMap<>();
        public List<T> findAll() { return new ArrayList<>(data.values()); }
        public Optional<T> findById(ID id) { return Optional.ofNullable(data.get(id)); }
        public void deleteById(ID id) { data.remove(id); }
        public void clearCache() { data.clear(); }
    }

    public static class Accounts extends BaseRepo<Account, UUID> implements AccountRepository {
        public Account save(Account e) { data.put(e.id(), e); return e; }

        /** Fake não modela COD_PERSON — testes de engine não cobrem guarda implícita (isso é BaseHttpTest). */
        public List<Account> findAllByPerson(String personId) { return findAll(); }
        public Optional<Account> findByIdAndPerson(UUID id, String personId) { return findById(id); }
    }

    public static class Transactions extends BaseRepo<Transaction, UUID> implements TransactionRepository {

        /** Vínculo F005_TRANSACTION_CATEGORY: transação → pessoa → categoria (tabela à parte, como no JDBC). */
        private final Map<UUID, Map<UUID, UUID>> categories = new LinkedHashMap<>();

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

        public void saveCategory(UUID transactionId, UUID personId, @Nullable UUID categoryId) {
            if (categoryId == null) {
                categories.getOrDefault(transactionId, new LinkedHashMap<>()).remove(personId);
                return;
            }
            categories.computeIfAbsent(transactionId, ignored -> new LinkedHashMap<>()).put(personId, categoryId);
        }

        public Optional<UUID> findCategory(UUID transactionId, UUID personId) {
            return Optional.ofNullable(categories.getOrDefault(transactionId, Map.of()).get(personId));
        }

        public Map<UUID, UUID> findCategoriesByPerson(UUID personId) {
            var byTransaction = new LinkedHashMap<UUID, UUID>();
            categories.forEach((transactionId, byPerson) -> {
                var categoryId = byPerson.get(personId);
                if (categoryId != null) byTransaction.put(transactionId, categoryId);
            });
            return byTransaction;
        }

        public void deleteCategoryByTransaction(UUID transactionId) { categories.remove(transactionId); }

        public List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
            return findCategoriesByPerson(personId).entrySet().stream()
                    .filter(entry -> categoryIds.contains(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();
        }

        public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
            categories.values().forEach(byPerson ->
                    byPerson.computeIfPresent(personId, (ignored, current) ->
                            oldCategoryId.equals(current) ? newCategoryId : current));
        }

        /** O vínculo de tag (F004_TRANSACTION_TAG) não é modelado neste fake. */
        public void reassignTag(UUID oldTagId, UUID newTagId, UUID personId) {
            // não exercido por estes testes
        }

        public void detachTag(UUID tagId, UUID personId) {
            // não exercido por estes testes
        }

        @Override
        public void clearCache() { data.clear(); categories.clear(); }

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

    public static class CostCenters extends BaseRepo<CostCenter, UUID> implements CostCenterRepository {
        public CostCenter save(CostCenter e) { data.put(e.id(), e); return e; }
    }

    /** Balance tem chave de negócio (accountId, period) — sem personId próprio, não cabe em BaseRepo. */
    public static class Balances implements BalanceRepository {
        private final List<Balance> data = new ArrayList<>();
        private final Set<UUID> dirtyAccountIds = new HashSet<>();

        public List<Balance> findAll() { return new ArrayList<>(data); }

        /** Sem personId próprio: nada aqui indexa por UUID solto — ver {@link #delete}. */
        public Optional<Balance> findById(UUID id) { return Optional.empty(); }

        public Balance save(Balance e) {
            data.removeIf(b -> b.account().id().equals(e.account().id()) && b.period().equals(e.period()));
            data.add(e);
            dirtyAccountIds.remove(e.account().id());
            return e;
        }

        public void deleteById(UUID id) { /* sem personId próprio; ver delete(accountId, period) */ }

        /** Linha some junto com FLG_DIRTY (mesma semântica do DELETE real: nada sobra pra estar sujo). */
        public void delete(UUID accountId, YearMonth period) {
            data.removeIf(b -> b.account().id().equals(accountId) && b.period().equals(period));
            if (data.stream().noneMatch(b -> b.account().id().equals(accountId))) {
                dirtyAccountIds.remove(accountId);
            }
        }

        public List<Balance> findByAccount(UUID accountId) {
            return data.stream().filter(b -> b.account().id().equals(accountId)).toList();
        }

        public void markDirty(UUID accountId) { dirtyAccountIds.add(accountId); }

        public List<UUID> findDirtyAccountIds() { return List.copyOf(dirtyAccountIds); }

        public void clearCache() { data.clear(); dirtyAccountIds.clear(); }
    }

    public static class Cards extends BaseRepo<CreditCard, UUID> implements CreditCardRepository {
        public CreditCard save(CreditCard e) { data.put(e.id(), e); return e; }

        /** Fake não modela COD_PERSON — testes de engine não cobrem guarda implícita (isso é BaseHttpTest). */
        public List<CreditCard> findAllByPerson(String personId) { return findAll(); }
        public List<CreditCard> findByAccountAndPerson(UUID accountId, String personId) {
            return findAll().stream().filter(c -> accountId.equals(c.accountId())).toList();
        }
    }

    /** Ao contrário dos demais fakes, este modela {@code COD_PERSON}: {@code personId} é campo do
     *  próprio {@link Tag}, então filtrar por pessoa aqui é fiel ao adaptador JDBC. */
    public static class Tags extends BaseRepo<Tag, UUID> implements TagRepository {
        public Tag save(Tag e) { data.put(e.id(), e); return e; }

        public List<Tag> findAllByPerson(UUID personId) {
            return findAll().stream().filter(t -> personId.equals(t.personId())).toList();
        }

        public Optional<Tag> findByPersonAndId(UUID personId, UUID id) {
            return findById(id).filter(t -> personId.equals(t.personId()));
        }
    }

    /** Junção transação×tag por pessoa ({@code F004_TRANSACTION_TAG}). */
    public static class TransactionTags implements TransactionTagRepository {
        private record Link(UUID personId, UUID transactionId, UUID tagId) {}

        private final List<Link> links = new ArrayList<>();

        void link(UUID personId, UUID transactionId, UUID tagId) { links.add(new Link(personId, transactionId, tagId)); }

        public List<UUID> findTransactionIdsByTag(UUID personId, UUID tagId) {
            return links.stream()
                    .filter(l -> personId.equals(l.personId()) && tagId.equals(l.tagId()))
                    .map(Link::transactionId)
                    .toList();
        }

        public void reassignTag(UUID oldTagId, UUID newTagId, UUID personId) {
            val reassigned = links.stream()
                    .filter(l -> personId.equals(l.personId()) && oldTagId.equals(l.tagId()))
                    .map(l -> new Link(l.personId(), l.transactionId(), newTagId))
                    .filter(l -> !links.contains(l)) // dedupe: já vinculada ao destino
                    .toList();
            links.removeIf(l -> personId.equals(l.personId()) && oldTagId.equals(l.tagId()));
            links.addAll(reassigned);
        }

        public void deleteByTag(UUID personId, UUID tagId) {
            links.removeIf(l -> personId.equals(l.personId()) && tagId.equals(l.tagId()));
        }

        public void deleteByTransaction(UUID transactionId) {
            links.removeIf(l -> transactionId.equals(l.transactionId()));
        }

        public void clearCache() { links.clear(); }
    }

    /** Como o fake de tag, modela {@code COD_PERSON} — {@code personId} é campo do próprio
     *  {@link Category}. */
    public static class Categories extends BaseRepo<Category, UUID> implements CategoryRepository {
        public Category save(Category e) { data.put(e.id(), e); return e; }

        public List<Category> findAllByPerson(UUID personId) {
            return findAll().stream().filter(c -> personId.equals(c.personId())).toList();
        }

        public List<Category> findByNature(UUID personId, Transaction.Type nature) {
            return findAllByPerson(personId).stream().filter(c -> c.nature() == nature).toList();
        }
    }

    /** Pessoas ({@code F000_PERSON}) — o kernel f000 é dono. */
    public static class People extends BaseRepo<Person, UUID> implements PersonRepository {
        public Person save(Person e) { data.put(e.id(), e); return e; }
    }

    public static class Closings implements ClosingRepository {
        private @Nullable YearMonth ym;
        public Optional<YearMonth> find() { return Optional.ofNullable(ym); }
        public void save(YearMonth ym) { this.ym = ym; }
        public void clear() { this.ym = null; }
    }
}

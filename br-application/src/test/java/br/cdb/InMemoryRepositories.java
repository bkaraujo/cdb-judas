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
import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f005._0_domain.repository.CategoryRepository;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._0_domain.repository.TransactionCategoryRepository;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.cdb.feature.f006._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._0_domain.repository.ImportRuleRepository;
import br.cdb.feature.f010._0_domain.repository.ImportRuleTriggerRepository;
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

        public Transaction save(Transaction e) { data.put(e.id(), e); return e; }

        /** Fake não modela COD_PERSON — testes de engine não cobrem guarda implícita (isso é BaseHttpTest). */
        public List<Transaction> findAllByPerson(String personId) { return findAll(); }
        public Optional<Transaction> findByIdAndPerson(UUID id, String personId) { return findById(id); }

        public void reassignAccount(UUID from, UUID to) {
            for (var t : List.copyOf(data.values())) {
                if (t.accountId().equals(from)) {
                    data.put(t.id(), new Transaction(t.id(), t.description(), t.signal(), t.amount(), t.purchasedAt(),
                            to, t.status(), t.planned(), t.paymentDate(), t.groupId(),
                            t.installmentNumber(), t.totalInstallments(), t.notes(), t.createdAt(), t.updatedAt(), t.cardId()));
                }
            }
        }

        public void reassignCard(UUID from, UUID to) {
            for (var t : List.copyOf(data.values())) {
                if (from.equals(t.cardId())) {
                    data.put(t.id(), new Transaction(t.id(), t.description(), t.signal(), t.amount(), t.purchasedAt(),
                            t.accountId(), t.status(), t.planned(), t.paymentDate(), t.groupId(),
                            t.installmentNumber(), t.totalInstallments(), t.notes(), t.createdAt(), t.updatedAt(), to));
                }
            }
        }
    }

    /** Vínculo F006_TRANSACTION_CATEGORY: transação → pessoa → categoria (tabela à parte, como no JDBC). */
    public static class TransactionCategories implements TransactionCategoryRepository {
        private final Map<UUID, Map<UUID, UUID>> categories = new LinkedHashMap<>();

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

        public void clearCache() { categories.clear(); }
    }

    /** Vínculo F006_TRANSACTION_TAG: transação → pessoa → tags (tabela à parte, como no JDBC). */
    public static class TransactionTags implements TransactionTagRepository {
        private final Map<UUID, Map<UUID, List<UUID>>> tags = new LinkedHashMap<>();

        public void replaceTags(UUID transactionId, UUID personId, List<UUID> tagIds) {
            if (tagIds.isEmpty()) {
                tags.getOrDefault(transactionId, new LinkedHashMap<>()).remove(personId);
                return;
            }
            tags.computeIfAbsent(transactionId, ignored -> new LinkedHashMap<>()).put(personId, List.copyOf(tagIds));
        }

        public Map<UUID, List<UUID>> findTagsByPerson(UUID personId) {
            var byTransaction = new LinkedHashMap<UUID, List<UUID>>();
            tags.forEach((transactionId, byPerson) -> {
                var tagIds = byPerson.get(personId);
                if (tagIds != null && !tagIds.isEmpty()) byTransaction.put(transactionId, tagIds);
            });
            return byTransaction;
        }

        public void deleteTagsByTransaction(UUID transactionId) { tags.remove(transactionId); }

        public void reassignTag(UUID oldTagId, UUID newTagId, UUID personId) {
            tags.values().forEach(byPerson -> byPerson.computeIfPresent(personId, (ignored, current) -> {
                if (!current.contains(oldTagId)) return current;
                val updated = new ArrayList<>(current);
                updated.replaceAll(id -> oldTagId.equals(id) ? newTagId : id);
                // dedupe: a transação já pode estar vinculada ao destino.
                return updated.stream().distinct().toList();
            }));
        }

        public void detachTag(UUID tagId, UUID personId) {
            tags.values().forEach(byPerson -> byPerson.computeIfPresent(personId,
                    (ignored, current) -> current.stream().filter(id -> !tagId.equals(id)).toList()));
        }

        public void clearCache() { tags.clear(); }
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

        public void markAllDirty() { data.forEach(b -> dirtyAccountIds.add(b.account().id())); }

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

    public static class ImportRules extends BaseRepo<ImportRule, UUID> implements ImportRuleRepository {
        public ImportRule save(ImportRule e) { data.put(e.id(), e); return e; }

        public List<ImportRule> findAllByPerson(UUID personId) {
            return findAll().stream().filter(r -> personId.equals(r.personId())).toList();
        }

        public Optional<ImportRule> findByPersonAndId(UUID personId, UUID id) {
            return findById(id).filter(r -> personId.equals(r.personId()));
        }
    }

    /** Vínculo F010_IMPORT_RULE_TRIGGER: regra → pessoa → gatilhos (tabela à parte, como no JDBC). */
    public static class ImportRuleTriggers implements ImportRuleTriggerRepository {
        private final Map<UUID, Map<UUID, List<String>>> triggers = new LinkedHashMap<>();

        public void replaceTriggers(UUID ruleId, UUID personId, List<String> triggerList) {
            if (triggerList.isEmpty()) {
                triggers.getOrDefault(ruleId, new LinkedHashMap<>()).remove(personId);
                return;
            }
            triggers.computeIfAbsent(ruleId, ignored -> new LinkedHashMap<>()).put(personId, List.copyOf(triggerList));
        }

        public Map<UUID, List<String>> findTriggersByPerson(UUID personId) {
            var byRule = new LinkedHashMap<UUID, List<String>>();
            triggers.forEach((ruleId, byPerson) -> {
                var triggerList = byPerson.get(personId);
                if (triggerList != null && !triggerList.isEmpty()) byRule.put(ruleId, triggerList);
            });
            return byRule;
        }

        public void deleteTriggersByRule(UUID ruleId) { triggers.remove(ruleId); }

        public void clearCache() { triggers.clear(); }
    }

    /** Como o fake de tag, modela {@code COD_PERSON} — {@code personId} é campo do próprio
     *  {@link Category}. */
    public static class Categories extends BaseRepo<Category, UUID> implements CategoryRepository {
        public Category save(Category e) { data.put(e.id(), e); return e; }

        public List<Category> findAllByPerson(UUID personId) {
            return findAll().stream().filter(c -> personId.equals(c.personId())).toList();
        }

        public List<Category> findByNature(UUID personId, Nature nature) {
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

package br.cdb.feature.f006._1_application.usecase;

import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f005.F005Api;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.service.TransactionCategoryService;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.cdb.feature.f006._1_application.service.TransactionTagService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.*;

/**
 * Toda a leitura de transação da fatia {@code f006} — o par de {@link WriteUseCases}. Context-wired
 * como as demais classes ex-contexto ({@code Context.tryGet(ReadUseCases.class)}, nunca {@code @Inject}).
 *
 * <p>A guarda de propriedade (anti-IDOR) da listagem vive aqui, não mais numa camada de fronteira
 * acima: {@link UserGuards} é bean CDI resolvido pelo {@code Context} (publicado por
 * {@code F006Module} no {@code StartupEvent}) e alcançado <b>sob demanda</b> — é
 * {@code @RequestScoped}, então só pode ser tocado dentro de uma requisição. As consultas por
 * {@code personId} têm, além disso, a <b>guarda implícita</b> do
 * {@code F006_TRANSACTION.COD_PERSON} no WHERE.
 *
 * <p>Leitura cross-slice não mora mais aqui: fechamento contábil vem de {@code f002.F002Api} e
 * categoria de transferência de {@code f005.F005Api}, os clientes tipados que cada fatia dona
 * publica sobre {@code f000.InternalApi}.
 */
@NullMarked
public class ReadUseCases {

    private final TransactionService service = Context.tryGet(TransactionService.class);
    private final TransactionCategoryService categoryService = Context.tryGet(TransactionCategoryService.class);
    private final TransactionTagService tagService = Context.tryGet(TransactionTagService.class);

    /** Bean CDI resolvido a cada chamada: {@code @RequestScoped}, nunca guardado em campo. */
    private static UserGuards guards() {
        return Context.get(UserGuards.class);
    }

    /** Filtro da listagem HTTP; campos nulos não filtram. {@code limit} ≤ 0 também não pagina. */
    @NullMarked
    public record TransactionFilter(@Nullable UUID accountId, @Nullable Integer limit,
                                    @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo,
                                    @Nullable String status, @Nullable String type) {}

    public Result<List<Transaction>, BusinessError> transactions(UUID accountId) {
        val all = service.findByAccount(accountId).stream()
                .sorted(Comparator.comparing(Transaction::date).reversed())
                .toList();
        return Result.success(all);
    }

    public Result<List<Transaction>, BusinessError> transactions() {
        val all = service.findAll().stream()
                .sorted(Comparator.comparing(Transaction::date).reversed())
                .toList();
        return Result.success(all);
    }

    /** Guarda implícita: só as transações de {@code personId} — a versão que a leitura externa (listagem) usa. */
    public Result<List<Transaction>, BusinessError> transactions(String personId) {
        val all = service.findAllByPerson(personId).stream()
                .sorted(Comparator.comparing(Transaction::date).reversed())
                .toList();
        return Result.success(all);
    }

    /**
     * Listagem filtrada + paginada da pessoa, com a categoria de cada transação já resolvida num
     * único SELECT ({@link #categoriesByPerson}). Filtrar por conta exige ser dono dela
     * ({@link UserGuards#ownsAccount}) — 404 natural quando não é.
     */
    public Result<List<Transaction>, BusinessError> transactions(UUID personId, TransactionFilter filter) {
        val guard = filter.accountId() == null
                ? Result.<BusinessError>success()
                : guards().ownsAccount(filter.accountId());

        return guard.map(ignored -> filtered(personId, filter));
    }

    private List<Transaction> filtered(UUID personId, TransactionFilter filter) {
        val accountId = filter.accountId();
        val dateFrom = filter.dateFrom();
        val dateTo = filter.dateTo();
        val status = filter.status();
        val type = filter.type();
        val limit = filter.limit();

        val categories = categoriesByPerson(personId);
        val tags = tagsByPerson(personId);
        
        val f005 = Context.get(F005Api.class);
        val natureCache = new HashMap<UUID, Nature>();
        
        val filtered = transactions(personId.toString()).getOrElse(List.of()).stream()
                .filter(t -> accountId == null || accountId.equals(t.accountId()))
                .filter(t -> dateFrom == null || !t.date().isBefore(dateFrom))
                .filter(t -> dateTo == null || !t.date().isAfter(dateTo))
                .filter(t -> status == null || status.equalsIgnoreCase(t.status().name()))
                .filter(t -> {
                    if (type == null) return true;
                    val catId = categories.get(t.id());
                    val nat = catId != null ? natureCache.computeIfAbsent(catId, f005::natureOf) : Nature.EXPENSE;
                    val sig = t.calculateSignal(nat);
                    return type.equalsIgnoreCase((sig > 0 ? Nature.INCOME : Nature.EXPENSE).name());
                })
                .toList();
        val page = (limit != null && limit > 0 && limit < filtered.size())
                ? filtered.subList(0, limit)
                : filtered;
        return page.stream()
                .map(t -> t.withCategory(categories.get(t.id())).withTags(tags.getOrDefault(t.id(), List.of())))
                .toList();
    }

    /** Membros do grupo (parcelas ou pernas de transferência) da pessoa — inclui a própria origem. */
    public List<Transaction> transactionsInGroup(UUID groupId, UUID personId) {
        return transactions(personId.toString()).getOrElse(List.of()).stream()
                .filter(t -> groupId.equals(t.groupId()))
                .toList();
    }

    /**
     * Pernas irmãs de transferência de {@code t} (mesmo groupId, com uma perna INCOME e uma EXPENSE
     * no grupo) — lista vazia se {@code t} não tem groupId ou se o grupo não é uma transferência
     * (ex.: grupo de parcelas, tipo único). Mesma heurística de tipos mistos de
     * {@code TransactionService.findTransferSiblings} e {@code isTransfer()}
     * (web/pages/transactions/actions.js), aqui restrita às transações da pessoa.
     */
    public List<Transaction> transferSiblingsOf(Transaction t, UUID personId) {
        val groupId = t.groupId();
        if (groupId == null) return List.of();
        val group = transactionsInGroup(groupId, personId);
        
        val f005 = Context.get(F005Api.class);
        val categories = categoriesByPerson(personId);
        val natureCache = new HashMap<UUID, Nature>();
        
        val hasIncome = group.stream().anyMatch(x -> {
            val catId = categories.get(x.id());
            val nat = catId != null ? natureCache.computeIfAbsent(catId, f005::natureOf) : Nature.EXPENSE;
            return x.calculateSignal(nat) > 0;
        });
        val hasExpense = group.stream().anyMatch(x -> {
            val catId = categories.get(x.id());
            val nat = catId != null ? natureCache.computeIfAbsent(catId, f005::natureOf) : Nature.EXPENSE;
            return x.calculateSignal(nat) < 0;
        });
        
        if (!hasIncome || !hasExpense) return List.of();
        return group.stream().filter(x -> !x.id().equals(t.id())).toList();
    }

    public Result<List<Transaction>, BusinessError> pending() {
        return Result.success(service.findPending());
    }

    public Result<Transaction, BusinessError> transaction(UUID id) {
        return service.findById(id);
    }

    /** Guarda implícita: vazio (404) se {@code id} existe mas não pertence a {@code personId}. */
    public Result<Transaction, BusinessError> transaction(UUID id, String personId) {
        return service.findByIdAndPerson(id, personId);
    }

    // ── Vínculo transação↔categoria (F006_TRANSACTION_CATEGORY) ────────────────
    // Tabela à parte de F006_TRANSACTION: Transaction.categoryId não vem do save/read da engine.

    /** {@code tx} com o vínculo de categoria já resolvido — {@code null} quando não há vínculo. */
    public Transaction withCategory(Transaction tx, UUID personId) {
        return tx.withCategory(categoryService.findCategory(tx.id(), personId).orElse(null));
    }

    /** Categoria por transação, para resolver uma listagem inteira num único SELECT. */
    public Map<UUID, UUID> categoriesByPerson(UUID personId) {
        return categoryService.findCategoriesByPerson(personId);
    }

    public List<UUID> transactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        return categoryService.findTransactionIdsByCategories(personId, categoryIds);
    }

    // ── Vínculo transação↔tag (F006_TRANSACTION_TAG) ───────────────────────────

    /** Tags por transação, para resolver uma listagem inteira num único SELECT. */
    public Map<UUID, List<UUID>> tagsByPerson(UUID personId) {
        return tagService.findTagsByPerson(personId);
    }
}

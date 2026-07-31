package br.cdb.feature.f006._1_application.usecase;

import br.cdb.feature.f000._1_application.InternalApi;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.service.TransactionService;
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
 * acima: {@link UserGuards} e {@link InternalApi} são beans CDI resolvidos pelo {@code Context}
 * (publicados por {@code F006Module} no {@code StartupEvent}) e alcançados <b>sob demanda</b> —
 * {@code UserGuards} é {@code @RequestScoped}, então só pode ser tocado dentro de uma requisição.
 * As consultas por {@code personId} têm, além disso, a <b>guarda implícita</b> do
 * {@code F006_TRANSACTION.COD_PERSON} no WHERE.
 */
@NullMarked
public class ReadUseCases {

    private final TransactionService service = Context.tryGet(TransactionService.class);

    /** Corpo mínimo do endpoint interno {@code GET /categories/transfer} (f005) — zero tipo
     *  cross-slice, mesma regra dos antigos ports. */
    @NullMarked
    private record TransferCategoryDto(UUID id) {}

    /** Corpo mínimo do endpoint público {@code GET /accounts/closing} (f002) — zero tipo
     *  cross-slice, mesma regra dos antigos ports. */
    @NullMarked
    private record ClosingDto(@Nullable String period) {}

    /** Bean CDI resolvido a cada chamada: {@code @RequestScoped}, nunca guardado em campo. */
    private static UserGuards guards() {
        return Context.get(UserGuards.class);
    }

    private static InternalApi internalApi() {
        return Context.get(InternalApi.class);
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
        val filtered = transactions(personId.toString()).getOrElse(List.of()).stream()
                .filter(t -> accountId == null || accountId.equals(t.accountId()))
                .filter(t -> dateFrom == null || !t.date().isBefore(dateFrom))
                .filter(t -> dateTo == null || !t.date().isAfter(dateTo))
                .filter(t -> status == null || status.equalsIgnoreCase(t.status().name()))
                .filter(t -> type == null || type.equalsIgnoreCase(t.type().name()))
                .toList();
        val page = (limit != null && limit > 0 && limit < filtered.size())
                ? filtered.subList(0, limit)
                : filtered;
        return page.stream().map(t -> t.withCategory(categories.get(t.id()))).toList();
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
        val hasIncome = group.stream().anyMatch(x -> x.type() == Transaction.Type.INCOME);
        val hasExpense = group.stream().anyMatch(x -> x.type() == Transaction.Type.EXPENSE);
        if (!hasIncome || !hasExpense) return List.of();
        return group.stream().filter(x -> !x.id().equals(t.id())).toList();
    }

    // ── Leitura cross-slice síncrona (InternalApi, HTTP real contra a fatia dona) ───────

    /** Categoria de sistema de transferência da natureza pedida — endpoint público de f005. */
    public UUID transferCategoryId(Transaction.Type nature) {
        return internalApi().get("/categories/transfer?nature=" + nature.name(), TransferCategoryDto.class).id();
    }

    /** Período de fechamento vigente ({@code yyyy-MM}) ou {@code null} — endpoint público de f002. */
    public @Nullable String closingPeriod() {
        return internalApi().get("/accounts/closing", ClosingDto.class).period();
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

    // ── Vínculo transação↔categoria (F005_TRANSACTION_CATEGORY) ────────────────
    // Tabela à parte de F006_TRANSACTION: Transaction.categoryId não vem do save/read da engine.

    /** {@code tx} com o vínculo de categoria já resolvido — {@code null} quando não há vínculo. */
    public Transaction withCategory(Transaction tx, UUID personId) {
        return tx.withCategory(service.findCategory(tx.id(), personId).orElse(null));
    }

    /** Categoria por transação, para resolver uma listagem inteira num único SELECT. */
    public Map<UUID, UUID> categoriesByPerson(UUID personId) {
        return service.findCategoriesByPerson(personId);
    }

    public List<UUID> transactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        return service.findTransactionIdsByCategories(personId, categoryIds);
    }
}

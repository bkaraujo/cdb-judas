package br.cdb.feature.f006._1_application;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._1_application.InternalApi;
import br.cdb.feature.f000._1_application.UserGuards;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.command.TransactionCommand;
import br.cdb.feature.f006._1_application.command.TransactionScope;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Use case da fatia {@code f006} (transactions + transfer). Nome coincide com o use case do
 * contexto monetário ({@code br.cdb.feature.f006._1_application.usecase.TransactionUseCase}) —
 * referenciado por FQN completo no campo {@code ucTransaction} para evitar colisão de import.
 *
 * <p>{@code deleteTransaction} não limpa o overlay/vínculo de tag diretamente: publica
 * {@link TransactionsDeleted} e deixa {@code TransactionOverlayListener} (aqui) e
 * {@code TagTransactionListener} (f004) reagirem, best-effort. SSE de conta publica
 * {@link AccountStreamEvents.Refresh} — dispatch é responsabilidade única de {@code f999}.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TransactionUseCase {

    private final br.cdb.feature.f006._1_application.usecase.TransactionUseCase ucTransaction =
            Registry.tryGet(br.cdb.feature.f006._1_application.usecase.TransactionUseCase.class);

    private final UserGuards guards;
    private final InternalApi internalApi;

    /** Corpo mínimo do endpoint interno {@code GET /categories/transfer} (f005) — zero tipo
     *  cross-slice, mesma regra dos antigos ports. */
    @NullMarked
    private record TransferCategoryDto(UUID id) {}

    /** Corpo mínimo do endpoint público {@code GET /accounts/closing} (f002) — zero tipo
     *  cross-slice, mesma regra dos antigos ports. */
    @NullMarked
    private record ClosingDto(@Nullable String period) {}

    @NullMarked
    public record TransactionFilter(@Nullable UUID accountId, @Nullable Integer limit,
                                    @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo,
                                    @Nullable String status, @Nullable String type) {}

    /** IDs das transações da pessoa vinculadas a qualquer categoria de {@code categoryIds} — consumido
     *  via {@code InternalApi} por {@code CategoryUseCase} (f005) na exclusão de categoria. */
    public List<UUID> transactionIdsByCategories(UUID personId, List<UUID> categoryIds) {
        return ucTransaction.transactionIdsByCategories(personId, categoryIds);
    }

    public Result<List<Transaction>, BusinessError> transactions(UUID personId, TransactionFilter filter) {
        val accountId = filter.accountId();
        val guard = accountId == null ? Result.<BusinessError>success() : guards.ownsAccount(accountId);

        return guard.flatMap(ignored -> {
            val dateFrom = filter.dateFrom();
            val dateTo = filter.dateTo();
            val status = filter.status();
            val type = filter.type();
            val limit = filter.limit();

            return ucTransaction.transactions(personId.toString()).map(all -> {
                val categories = ucTransaction.categoriesByPerson(personId);
                val filtered = all.stream()
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
            });
        });
    }

    public Result<Transaction, BusinessError> createTransaction(UUID personId, TransactionCommand.Create cmd, @Nullable UUID categoryId) {
        if (guards.ownsAccountAndCard(cmd.accountId(), cmd.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (validateClosing(cmd.date()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return ucTransaction.upsert(cmd).map(t -> {
            // Vincula a categoria da primeira parcela e depois a das irmãs do grupo
            ucTransaction.saveCategory(t.id(), personId, categoryId);
            saveCategoryForGroup(t, personId, categoryId);
            MessageBus.submit(new AccountStreamEvents.Refresh(t.accountId(), personId.toString()));
            return t.withCategory(categoryId);
        });
    }

    public Result<Transaction, BusinessError> updateTransaction(UUID personId, TransactionCommand.Update cmd, @Nullable UUID categoryId) {
        if (guards.ownsAccountAndCard(cmd.accountId(), cmd.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }

        val txId = cmd.id();
        UUID previous = null;
        if (ucTransaction.transaction(txId) instanceof Result.Success(var existing)) {
            if (guards.ownsAccount(existing.accountId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            val guard = validateClosing(existing.date(), cmd.date());
            if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
            previous = existing.accountId();
        }

        val previousAccountId = previous;
        return ucTransaction.upsert(cmd).map(t -> {
            val hadCategory = ucTransaction.withCategory(t, personId).categoryId() != null;
            ucTransaction.saveCategory(t.id(), personId, categoryId);
            val transferSiblings = transferSiblingsOf(t, personId);
            // Se for grupo de parcelas (nunca transferência — pernas de transferência carregam
            // categoria por natureza da própria perna, nunca a da perna editada; ver
            // saveTransferGroupCategories/transfer()): atualiza a categoria de todos os membros.
            if (t.groupId() != null && !hadCategory && transferSiblings.isEmpty()) {
                saveCategoryForGroup(t, personId, categoryId);
            }
            publishAccountUpdate(personId, t.accountId(), previousAccountId, transferSiblings);
            return t.withCategory(categoryId);
        });
    }

    public Result<Transaction, BusinessError> updateTransactionStatus(
            UUID personId, UUID txId, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return ucTransaction.transaction(txId)
                .flatMap(existing -> guards.ownsAccount(existing.accountId()))
                .flatMap(ignored -> ucTransaction.updateTransactionStatus(txId, status, paymentDate).map(t -> {
                    MessageBus.submit(new AccountStreamEvents.Refresh(t.accountId(), personId.toString()));
                    return ucTransaction.withCategory(t, personId);
                }));
    }

    public Result<Void, BusinessError> deleteTransaction(UUID txId, TransactionScope scope) {
        UUID accountId = null;
        if (ucTransaction.transaction(txId) instanceof Result.Success(var existing)) {
            if (guards.ownsAccount(existing.accountId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            if (validateClosing(existing.date()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            accountId = existing.accountId();
        }

        val affected = accountId;
        return ucTransaction.delete(new TransactionCommand.Delete(txId, scope)).flatMap(ids -> {
            MessageBus.submit(new TransactionsDeleted(ids));
            if (affected != null) MessageBus.submit(new AccountStreamEvents.Refresh(affected, HTTPRequest.personId()));
            return Result.success();
        });
    }

    public Result<Transaction, BusinessError> transfer(UUID personId, UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (guards.ownsAccounts(fromAccountId, toAccountId) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (validateClosing(date) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        // F005_TRANSACTION_CATEGORY.COD_CATEGORY é NOT NULL. Cada perna recebe a categoria de sistema
        // "9. Outros / Transferência" da sua natureza: a saída (EXPENSE) a de despesa, a entrada
        // (INCOME) a de receita. Cobre as duas pernas do grupo, mantendo o 1:1 com MON_TRANSACTION.
        val expenseCategoryId = transferCategoryId(Transaction.Type.EXPENSE);
        val incomeCategoryId = transferCategoryId(Transaction.Type.INCOME);
        return ucTransaction.createTransfer(fromAccountId, toAccountId, date, amount).map(t -> {
            val categoryId = transferCategoryFor(t, expenseCategoryId, incomeCategoryId);
            ucTransaction.saveCategory(t.id(), personId, categoryId);
            saveTransferGroupCategories(t, personId, expenseCategoryId, incomeCategoryId);
            MessageBus.submit(new AccountStreamEvents.Refresh(fromAccountId, personId.toString()));
            MessageBus.submit(new AccountStreamEvents.Refresh(toAccountId, personId.toString()));
            return t.withCategory(categoryId);
        });
    }

    /**
     * Publica a conta atual, a conta anterior (se mudou) e — quando a transação editada é perna de
     * transferência — a(s) conta(s) da(s) perna(s) irmã(s): {@code updateTransfer} (contexto
     * monetário) espelha data/valor/status na perna oposta, uma mutação real fora da conta da perna
     * editada.
     */
    private void publishAccountUpdate(UUID personId, UUID accountId, @Nullable UUID previousAccountId,
                                       List<Transaction> transferSiblings) {
        MessageBus.submit(new AccountStreamEvents.Refresh(accountId, personId.toString()));
        if (previousAccountId != null && !previousAccountId.equals(accountId)) {
            MessageBus.submit(new AccountStreamEvents.Refresh(previousAccountId, personId.toString()));
        }
        for (val sib : transferSiblings) {
            MessageBus.submit(new AccountStreamEvents.Refresh(sib.accountId(), personId.toString()));
        }
    }

    /**
     * Pernas irmãs de transferência de {@code t} (mesmo groupId, com uma perna INCOME e uma EXPENSE
     * no grupo) — lista vazia se {@code t} não tem groupId ou se o grupo não é uma transferência
     * (ex.: grupo de parcelas, tipo único). Mesma heurística de tipos mistos de
     * {@code TransactionService.findTransferSiblings} (contexto monetário) e {@code isTransfer()}
     * (web/pages/transactions/actions.js), duplicada aqui de propósito: f006 não tem hoje um jeito
     * de perguntar "é transferência" ao contexto sem crescer a superfície pública da Facade por
     * causa de um único chamador.
     */
    private List<Transaction> transferSiblingsOf(Transaction t, UUID personId) {
        val groupId = t.groupId();
        if (groupId == null) return List.of();
        val group = transactionsInGroup(groupId, personId);
        val hasIncome = group.stream().anyMatch(x -> x.type() == Transaction.Type.INCOME);
        val hasExpense = group.stream().anyMatch(x -> x.type() == Transaction.Type.EXPENSE);
        if (!hasIncome || !hasExpense) return List.of();
        return group.stream().filter(x -> !x.id().equals(t.id())).toList();
    }

    private List<Transaction> transactionsInGroup(UUID groupId, UUID personId) {
        return ucTransaction.transactions(personId.toString()).getOrElse(List.of()).stream()
                .filter(t -> groupId.equals(t.groupId()))
                .toList();
    }

    private void saveCategoryForGroup(Transaction first, UUID personId, @Nullable UUID categoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        transactionsInGroup(groupId, personId).stream()
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> ucTransaction.saveCategory(t.id(), personId, categoryId));
    }

    /** Categoria de sistema de transferência da natureza pedida — leitura cross-slice síncrona via
     *  {@code InternalApi} (endpoint público de f005). */
    private UUID transferCategoryId(Transaction.Type nature) {
        return internalApi.get("/categories/transfer?nature=" + nature.name(), TransferCategoryDto.class).id();
    }

    /** Guarda de período fechado — leitura cross-slice síncrona via {@code InternalApi} (endpoint
     *  público de f002). Busca o fechamento uma vez e valida todas as datas passadas localmente
     *  (ex.: {@code updateTransaction} troca de conta e checa data antiga + nova), evitando um
     *  round-trip por data. */
    private Result<Void, BusinessError> validateClosing(LocalDate... dates) {
        val period = internalApi.get("/accounts/closing", ClosingDto.class).period();
        if (period == null) return Result.success();

        val closing = YearMonth.parse(period);
        for (val date : dates) {
            if (!YearMonth.from(date).isAfter(closing)) {
                return Result.failure(new BusinessError.BusinessRule(
                        "Período fechado. Lançamentos até " + period + " não podem ser alterados."));
            }
        }
        return Result.success();
    }

    private static UUID transferCategoryFor(Transaction leg, UUID expenseCategoryId, UUID incomeCategoryId) {
        return leg.type() == Transaction.Type.EXPENSE ? expenseCategoryId : incomeCategoryId;
    }

    /** Aplica a categoria de transferência da natureza de cada perna às demais pernas do grupo. */
    private void saveTransferGroupCategories(Transaction first, UUID personId, UUID expenseCategoryId, UUID incomeCategoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        transactionsInGroup(groupId, personId).stream()
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> ucTransaction.saveCategory(t.id(), personId,
                        transferCategoryFor(t, expenseCategoryId, incomeCategoryId)));
    }
}

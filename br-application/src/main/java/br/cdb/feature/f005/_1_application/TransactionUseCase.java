package br.cdb.feature.f005._1_application;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.command.TransactionCommand;
import br.cdb.context.monetary._1_application.command.TransactionScope;
import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._1_application.ClosingService;
import br.cdb.feature.f000._1_application.UserGuards;
import br.cdb.feature.f002._0_domain.event.AccountEvents;
import br.cdb.feature.f004._1_application.UserCategoryService;
import br.cdb.feature.f005._0_domain.UserTransaction;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case da fatia {@code f005} (transactions + transfer). Nome coincide com o use case do
 * contexto monetário ({@code br.cdb.context.monetary._1_application.usecase.TransactionUseCase}) —
 * referenciado por FQN completo no campo {@code ucTransaction} para evitar colisão de import.
 *
 * <p>{@code deleteTransaction} não limpa o overlay/vínculo de tag diretamente: publica
 * {@link TransactionsDeleted} e deixa {@code TransactionOverlayListener} (aqui) e
 * {@code TagTransactionListener} (f008) reagirem, best-effort. SSE de conta publica
 * {@link AccountEvents.Refresh} — dispatch é responsabilidade única de {@code f999}.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TransactionUseCase {

    private final br.cdb.context.monetary._1_application.usecase.TransactionUseCase ucTransaction =
            MonetaryUseCases.ucTransaction();

    private final UserGuards guards;
    private final UserTransactionService userTransactionService;
    private final UserCategoryService userCategoryService;
    private final ClosingService closingService;

    @NullMarked
    public record TransactionView(Transaction transaction, @Nullable UserTransaction overlay) {}

    @NullMarked
    public record TransactionFilter(@Nullable UUID accountId, @Nullable Integer limit,
                                    @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo,
                                    @Nullable String status, @Nullable String type) {}

    public Result<List<TransactionView>, BusinessError> transactions(UUID personId, TransactionFilter filter) {
        val accountId = filter.accountId();
        val guard = accountId == null ? Result.<BusinessError>success() : guards.ownsAccount(accountId);

        return guard.flatMap(ignored -> {
            val dateFrom = filter.dateFrom();
            val dateTo = filter.dateTo();
            val status = filter.status();
            val type = filter.type();
            val limit = filter.limit();

            return ucTransaction.transactions().map(all -> {
                val overlays = userTransactionService.indexByTransaction(personId);
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
                return page.stream().map(t -> new TransactionView(t, overlays.get(t.id()))).toList();
            });
        });
    }

    public Result<TransactionView, BusinessError> createTransaction(UUID personId, TransactionCommand.Create cmd, @Nullable UUID categoryId) {
        if (guards.ownsAccountAndCard(cmd.accountId(), cmd.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (closingService.validateDate(cmd.date()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return ucTransaction.upsert(cmd).map(t -> {
            // Cria o PERSON_TRANSACTION da primeira parcela e depois o das irmãs do grupo
            val overlay = userTransactionService.save(t.id(), t.accountId(), personId, categoryId);
            saveUserTransactionForGroup(t, personId, categoryId);
            MessageBus.submit(new AccountEvents.Refresh(t.accountId(), personId.toString()));
            return new TransactionView(t, overlay);
        });
    }

    public Result<TransactionView, BusinessError> updateTransaction(UUID personId, TransactionCommand.Update cmd, @Nullable UUID categoryId) {
        if (guards.ownsAccountAndCard(cmd.accountId(), cmd.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }

        val txId = cmd.id();
        UUID previous = null;
        if (ucTransaction.transaction(txId) instanceof Result.Success(var existing)) {
            if (guards.ownsAccount(existing.accountId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            val guard = closingService.validateDate(existing.date())
                    .flatMap(ignored -> closingService.validateDate(cmd.date()));
            if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
            previous = existing.accountId();
        }

        val previousAccountId = previous;
        return ucTransaction.upsert(cmd).map(t -> {
            // A PK do PERSON_TRANSACTION inclui a conta: se a atualização moveu a transação de conta,
            // a linha antiga da chave composta ficaria órfã (o save abaixo faria INSERT, não UPDATE).
            if (previousAccountId != null && !previousAccountId.equals(t.accountId())) {
                userTransactionService.deleteByTransactionAccountAndPerson(t.id(), previousAccountId, personId);
            }
            val existingOverlay = userTransactionService.find(t.id(), t.accountId(), personId);
            val overlay = userTransactionService.save(t.id(), t.accountId(), personId, categoryId);
            val transferSiblings = transferSiblingsOf(t);
            // Se for grupo de parcelas (nunca transferência — pernas de transferência carregam
            // categoria por natureza da própria perna, nunca a da perna editada; ver
            // saveTransferGroupCategories/transfer()): atualiza a categoria de todos os membros.
            if (t.groupId() != null && existingOverlay.isEmpty() && transferSiblings.isEmpty()) {
                saveUserTransactionForGroup(t, personId, categoryId);
            }
            publishAccountUpdate(personId, t.accountId(), previousAccountId, transferSiblings);
            return new TransactionView(t, overlay);
        });
    }

    public Result<TransactionView, BusinessError> updateTransactionStatus(
            UUID personId, UUID txId, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return ucTransaction.transaction(txId)
                .flatMap(existing -> guards.ownsAccount(existing.accountId()))
                .flatMap(ignored -> ucTransaction.updateTransactionStatus(txId, status, paymentDate).map(t -> {
                    val overlay = userTransactionService.find(t.id(), t.accountId(), personId).orElse(null);
                    MessageBus.submit(new AccountEvents.Refresh(t.accountId(), personId.toString()));
                    return new TransactionView(t, overlay);
                }));
    }

    public Result<Void, BusinessError> deleteTransaction(UUID txId, TransactionScope scope) {
        UUID accountId = null;
        if (ucTransaction.transaction(txId) instanceof Result.Success(var existing)) {
            if (guards.ownsAccount(existing.accountId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            if (closingService.validateDate(existing.date()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
            accountId = existing.accountId();
        }

        val affected = accountId;
        return ucTransaction.delete(new TransactionCommand.Delete(txId, scope)).map(ids -> {
            MessageBus.submit(new TransactionsDeleted(ids));
            if (affected != null) MessageBus.submit(new AccountEvents.Refresh(affected, HTTPRequest.personId()));
            return null;
        });
    }

    public Result<TransactionView, BusinessError> transfer(UUID personId, UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (guards.ownsAccounts(fromAccountId, toAccountId) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (closingService.validateDate(date) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        // PERSON_TRANSACTION.COD_CATEGORY é NOT NULL. Cada perna recebe a categoria de sistema
        // "9. Outros / Transferência" da sua natureza: a saída (EXPENSE) a de despesa, a entrada
        // (INCOME) a de receita. Cobre as duas pernas do grupo, mantendo o 1:1 com MON_TRANSACTION.
        val expenseCategoryId = userCategoryService.findOrCreateTransferCategory(personId, Transaction.Type.EXPENSE).id();
        val incomeCategoryId = userCategoryService.findOrCreateTransferCategory(personId, Transaction.Type.INCOME).id();
        return ucTransaction.createTransfer(fromAccountId, toAccountId, date, amount).map(t -> {
            val overlay = userTransactionService.save(t.id(), t.accountId(), personId, transferCategoryFor(t, expenseCategoryId, incomeCategoryId));
            saveTransferGroupCategories(t, personId, expenseCategoryId, incomeCategoryId);
            MessageBus.submit(new AccountEvents.Refresh(fromAccountId, personId.toString()));
            MessageBus.submit(new AccountEvents.Refresh(toAccountId, personId.toString()));
            return new TransactionView(t, overlay);
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
        MessageBus.submit(new AccountEvents.Refresh(accountId, personId.toString()));
        if (previousAccountId != null && !previousAccountId.equals(accountId)) {
            MessageBus.submit(new AccountEvents.Refresh(previousAccountId, personId.toString()));
        }
        for (val sib : transferSiblings) {
            MessageBus.submit(new AccountEvents.Refresh(sib.accountId(), personId.toString()));
        }
    }

    /**
     * Pernas irmãs de transferência de {@code t} (mesmo groupId, com uma perna INCOME e uma EXPENSE
     * no grupo) — lista vazia se {@code t} não tem groupId ou se o grupo não é uma transferência
     * (ex.: grupo de parcelas, tipo único). Mesma heurística de tipos mistos de
     * {@code TransactionService.findTransferSiblings} (contexto monetário) e {@code isTransfer()}
     * (web/pages/transactions/actions.js), duplicada aqui de propósito: f005 não tem hoje um jeito
     * de perguntar "é transferência" ao contexto sem crescer a superfície pública da Facade por
     * causa de um único chamador.
     */
    private List<Transaction> transferSiblingsOf(Transaction t) {
        val groupId = t.groupId();
        if (groupId == null) return List.of();
        val group = ucTransaction.transactions().getOrElse(List.of()).stream()
                .filter(x -> groupId.equals(x.groupId()))
                .toList();
        val hasIncome = group.stream().anyMatch(x -> x.type() == Transaction.Type.INCOME);
        val hasExpense = group.stream().anyMatch(x -> x.type() == Transaction.Type.EXPENSE);
        if (!hasIncome || !hasExpense) return List.of();
        return group.stream().filter(x -> !x.id().equals(t.id())).toList();
    }

    private void saveUserTransactionForGroup(Transaction first, UUID personId, @Nullable UUID categoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        ucTransaction.transactions().getOrElse(List.of()).stream()
                .filter(t -> groupId.equals(t.groupId()))
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> userTransactionService.save(t.id(), t.accountId(), personId, categoryId));
    }

    private static UUID transferCategoryFor(Transaction leg, UUID expenseCategoryId, UUID incomeCategoryId) {
        return leg.type() == Transaction.Type.EXPENSE ? expenseCategoryId : incomeCategoryId;
    }

    /** Aplica a categoria de transferência da natureza de cada perna às demais pernas do grupo. */
    private void saveTransferGroupCategories(Transaction first, UUID personId, UUID expenseCategoryId, UUID incomeCategoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        ucTransaction.transactions().getOrElse(List.of()).stream()
                .filter(t -> groupId.equals(t.groupId()))
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> userTransactionService.save(t.id(), t.accountId(), personId,
                        transferCategoryFor(t, expenseCategoryId, incomeCategoryId)));
    }
}

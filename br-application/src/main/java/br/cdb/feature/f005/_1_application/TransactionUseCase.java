package br.cdb.feature.f005._1_application;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.command.TransactionCommand;
import br.cdb.context.monetary._1_application.command.TransactionScope;
import br.cdb.core.web.Request;
import br.cdb.feature.f000._1_application.ClosingService;
import br.cdb.feature.f000._1_application.UserGuards;
import br.cdb.feature.f005._0_domain.UserTransaction;
import br.cdb.feature.f005._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f002._1_application.AccountStreamPublisher;
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
 * {@code TagTransactionListener} (f008) reagirem, best-effort. {@code accountStreamPublisher}
 * (SSE) continua chamada direta — transitório até f002 migrar (.claude/refactor.md).
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TransactionUseCase {

    private final br.cdb.context.monetary._1_application.usecase.TransactionUseCase ucTransaction =
            MonetaryUseCases.ucTransaction();

    private final UserGuards guards;
    private final UserTransactionService userTransactionService;
    private final ClosingService closingService;
    private final AccountStreamPublisher accountStreamPublisher;

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
            accountStreamPublisher.upsert(t.accountId());
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
            // Se for grupo de parcelas: atualiza a categoria de todos os membros
            if (t.groupId() != null && existingOverlay.isEmpty()) {
                saveUserTransactionForGroup(t, personId, categoryId);
            }
            publishAccountUpdate(t.accountId(), previousAccountId);
            return new TransactionView(t, overlay);
        });
    }

    public Result<TransactionView, BusinessError> updateTransactionStatus(
            UUID personId, UUID txId, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return ucTransaction.transaction(txId)
                .flatMap(existing -> guards.ownsAccount(existing.accountId()))
                .flatMap(ignored -> ucTransaction.updateTransactionStatus(txId, status, paymentDate).map(t -> {
                    val overlay = userTransactionService.find(t.id(), t.accountId(), personId).orElse(null);
                    accountStreamPublisher.upsert(t.accountId());
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
            if (affected != null) accountStreamPublisher.upsert(affected);
            return null;
        });
    }

    public Result<TransactionView, BusinessError> transfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (guards.ownsAccounts(fromAccountId, toAccountId) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        if (closingService.validateDate(date) instanceof Result.Failure<Void, BusinessError>(var error)) {
            return Result.failure(error);
        }
        return ucTransaction.createTransfer(fromAccountId, toAccountId, date, amount).map(t -> {
            accountStreamPublisher.upsert(fromAccountId);
            accountStreamPublisher.upsert(toAccountId);
            return new TransactionView(t, null);
        });
    }

    /** Publica a conta atual e, se a transação mudou de conta, também a conta anterior. */
    private void publishAccountUpdate(UUID accountId, @Nullable UUID previousAccountId) {
        accountStreamPublisher.upsert(accountId);
        if (previousAccountId != null && !previousAccountId.equals(accountId)) {
            accountStreamPublisher.upsert(previousAccountId);
        }
    }

    private void saveUserTransactionForGroup(Transaction first, UUID personId, @Nullable UUID categoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        ucTransaction.transactions().getOrElse(List.of()).stream()
                .filter(t -> groupId.equals(t.groupId()))
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> userTransactionService.save(t.id(), t.accountId(), personId, categoryId));
    }
}

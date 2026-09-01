package br.cdb.feature.f002._1_application.usecase;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.TransactionPolicy;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002._0_domain.DeletionQueue;
import br.cdb.feature.f002._0_domain.event.AccountEvents;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._1_application.command.AccountCommand;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f002._1_application.service.ClosingService;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@NullMarked
public class WriteUseCase {

    private final AccountService service = Context.tryGet(AccountService.class);
    private final BalanceService balanceService = Context.tryGet(BalanceService.class);
    private final CreditCardService creditCardService = Context.tryGet(CreditCardService.class);
    private final TransactionService transactionService = Context.tryGet(TransactionService.class);
    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);

    /** Bean CDI resolvido a cada chamada: {@code @RequestScoped}, nunca guardado em campo. */
    private static UserGuards guards() {
        return Context.get(UserGuards.class);
    }

    /** Porta implementada em {@code f999} e publicada no {@code Context} por {@code F999Module},
     *  depois do {@code DeletionQueueService} de que ela depende — resolvida por chamada. */
    private static DeletionQueue deletionQueue() {
        return Context.get(DeletionQueue.class);
    }

    /** Registrado por {@code F002Module} com {@code Context.set} — resolvido por chamada para que a
     *  construção da classe não dependa do módulo já ter rodado (testes de unidade). */
    private static ClosingService closingService() {
        return Context.get(ClosingService.class);
    }

    // ── Contas (entrada HTTP) ──────────────────────────────────────

    public Result<ReadUseCase.AccountAndTransactionsView, BusinessError> createAccount(AccountCommand.Create cmd, String color) {
        val personId = HTTPRequest.personId();
        return upsert(cmd, personId, color).map(account ->
                new ReadUseCase.AccountAndTransactionsView(account, List.of(), List.of()));
    }

    public Result<ReadUseCase.AccountAndTransactionsView, BusinessError> updateAccount(AccountCommand.Update cmd, String color) {
        return guards().ownsAccount(cmd.id()).flatMap(ignored -> {
            val personId = HTTPRequest.personId();
            return upsert(cmd, personId, color).map(account ->
                    new ReadUseCase.AccountAndTransactionsView(account, reads.cards(account.id(), personId), List.of()));
        });
    }

    /**
     * MOVE tem o alvo validado aqui primeiro (entrada da fatia) para nunca reatribuir transações
     * para uma conta inválida — {@link #delete} faz o re-key de fato ao apagar.
     */
    public Result<DeletionOutcome, BusinessError> deleteAccount(
            UUID personId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        val guard = strategy == DeletionStrategy.MOVE
                ? guards().ownsAccounts(id, Objects.requireNonNull(targetId))
                : guards().ownsAccount(id);
        if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);

        if (strategy == null) {
            val count = reads.transactionCount(personId, id);
            if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
        }

        if (strategy == DeletionStrategy.MOVE) {
            val target = Objects.requireNonNull(targetId);
            val targetCheck = validateAccountMoveTarget(id, target, personId);
            if (targetCheck instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
        }

        return delete(new AccountCommand.Delete(id, Deletions.toPolicy(strategy, targetId))).map(ids -> {
            MessageBus.submit(new AccountEvents.Deleted(id, personId.toString()));
            deletionQueue().enqueueAccountDeleted(id, personId);

            if (strategy != DeletionStrategy.MOVE) {
                MessageBus.submit(new TransactionsDeleted(ids));
                ids.forEach(txId -> deletionQueue().enqueueTransactionDeleted(txId, personId));
            }

            if (strategy == DeletionStrategy.MOVE) {
                val target = Objects.requireNonNull(targetId);
                balanceService.markDirty(target);
                MessageBus.submit(new AccountStreamEvents.Refresh(target, personId.toString()));
            }
            return new DeletionOutcome.Completed();
        });
    }

    private Result<Void, BusinessError> validateAccountMoveTarget(UUID sourceId, UUID targetId, UUID personId) {
        return reads.findAccount(targetId, personId.toString()).flatMap(target -> {
            if (target.id().equals(sourceId)) {
                return Result.failure(new BusinessError.BusinessRule("f002.account.transferTargetMustDiffer", targetId));
            }
            if (!target.active()) {
                return Result.failure(new BusinessError.BusinessRule("f002.account.transferTargetInactive", targetId));
            }
            return Result.success();
        });
    }

    // ── Fechamento ─────────────────────────────────────────────────

    public YearMonth saveClosing(YearMonth period) {
        return closingService().save(period);
    }

    public void clearClosing() {
        closingService().clear();
    }

    // ── Contas (engine — sem política de usuário) ──────────────────

    public Result<Account, BusinessError> upsert(AccountCommand.Upsert cmd, String personId, String color) {
        return switch (cmd) {
            case AccountCommand.Create(var name, var type, var active, var creditLimit, var overdraftLimit, var closingDay, var dueDay) ->
                    parse(UUID.randomUUID(), name, type, active, personId, color, creditLimit, overdraftLimit, closingDay, dueDay)
                            .map(service::save)
                            .ifSuccess(account -> MessageBus.submit(new AccountEvents.Created(account)));
            case AccountCommand.Update(var id, var name, var type, var active, var creditLimit, var overdraftLimit, var closingDay, var dueDay) ->
                    service.findById(id)
                            .flatMap(existing -> parse(id, name, type, active, personId, color, creditLimit, overdraftLimit, closingDay, dueDay))
                            .map(service::save)
                            .ifSuccess(account -> MessageBus.submit(new AccountEvents.Updated(account)));
        };
    }

    /** Ids das transações movidas/apagadas (vazio para {@link TransactionPolicy.Block}). */
    public Result<List<UUID>, BusinessError> delete(AccountCommand.Delete command) {
        return service.findById(command.id()).flatMap(account -> {
            Result<List<UUID>, BusinessError> result = switch (command.policy()) {
                case TransactionPolicy.Block ignored -> deleteBlock(account);
                case TransactionPolicy.Move(var targetId) -> deleteMove(account, targetId);
                case TransactionPolicy.Purge ignored -> deletePurge(account);
            };
            return result.ifSuccess(
                    _ -> MessageBus.submit(new AccountEvents.Deleted(command.id(), account.personId()))
            );
        });
    }

    private Result<List<UUID>, BusinessError> deleteBlock(Account account) {
        if (!transactionService.findByAccount(account.id()).isEmpty()) {
            return Result.failure(new BusinessError.Conflict("f002.account.hasLinkedTransactions", account.id()));
        }
        creditCardService.findByAccount(account.id()).forEach(c -> creditCardService.deleteById(c.id()));
        balanceService.findByAccount(account.id()).forEach(balanceService::deleteById);
        return service.deleteById(account.id()).map(ignored -> List.<UUID>of());
    }

    private Result<List<UUID>, BusinessError> deleteMove(Account account, UUID targetId) {
        return service.findById(targetId).flatMap(target -> {
            if (target.id().equals(account.id())) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("f002.account.transferTargetMustDiffer", targetId));
            }
            if (!target.active()) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("f002.account.transferTargetInactive", targetId));
            }

            val movedIds = transactionService.findByAccount(account.id()).stream().map(Transaction::id).toList();
            transactionService.reassignAccount(account.id(), target.id());

            creditCardService.findByAccount(account.id()).forEach(card ->
                    creditCardService.save(new CreditCard(card.id(), card.last4(), target.id(), card.active())));
            balanceService.findByAccount(account.id()).forEach(balanceService::deleteById);

            return service.deleteById(account.id()).map(ignored -> {
                balanceService.recalculate(target.id());
                return movedIds;
            });
        });
    }

    private Result<List<UUID>, BusinessError> deletePurge(Account account) {
        val ids = transactionService.findByAccount(account.id()).stream().map(Transaction::id).toList();
        ids.forEach(transactionService::deleteById);
        creditCardService.findByAccount(account.id()).forEach(c -> creditCardService.deleteById(c.id()));
        balanceService.findByAccount(account.id()).forEach(balanceService::deleteById);

        return service.deleteById(account.id()).map(ignored -> ids);
    }

    private Result<Account, BusinessError> parse(UUID accountId, String name, String type, boolean active,
                                                  String personId, String color,
                                                  @Nullable BigDecimal creditLimit, @Nullable BigDecimal overdraftLimit,
                                                  @Nullable Integer closingDay, @Nullable Integer dueDay) {
        val typeName = Strings.upper(type);
        val valid = Arrays.stream(Account.Type.values()).anyMatch(t -> t.name().equals(typeName));
        if (!valid) return Result.failure(new BusinessError.Validation("f002.account.unknownType", type));
        return Result.success(new Account(accountId, name, Account.Type.valueOf(typeName), active, personId, color,
                creditLimit, overdraftLimit, closingDay, dueDay, null, null));
    }
}

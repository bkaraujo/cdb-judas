package br.cdb.context.monetary._1_application.usecase;

import br.cdb.context.monetary._0_domain.event.AccountEvents;
import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.AccountBalance;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.command.AccountCommand;
import br.cdb.context.monetary._1_application.command.TransactionPolicy;
import br.cdb.context.monetary._1_application.service.*;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@NullMarked
public class AccountUseCase {

    private final AccountService accountService = Registry.tryGet(AccountService.class);
    private final BalanceService balanceService = Registry.tryGet(BalanceService.class);
    private final TransactionService transactionService = Registry.tryGet(TransactionService.class);
    private final CardService cardService = Registry.tryGet(CardService.class);
    private final BalanceRecalculationService balanceRecalculationService = Registry.tryGet(BalanceRecalculationService.class);

    public Result<List<Account>, BusinessError> listAccounts() {
        return Result.success(accountService.findAll());
    }

    public Result<Account, BusinessError> findAccount(UUID id) {
        return accountService.findById(id);
    }

    public Result<AccountBalance, BusinessError> getMonthlyBalance(UUID accountId, YearMonth period) {
        return accountService.findById(accountId)
                .flatMap(ignored -> balanceService.findByAccountAndPeriod(accountId, period));
    }

    public Result<List<AccountBalance>, BusinessError> getYearBalances(UUID accountId, int year) {
        return accountService.findById(accountId)
                .map(ignored -> balanceService.findByAccountAndYear(accountId, year));
    }

    public Result<Account, BusinessError> createAccount(AccountCommand cmd) {
        return parse(UUID.randomUUID(), cmd).map(accountService::save).ifSuccess(
                account -> MessageBus.submit(new AccountEvents.Created(account))
        );
    }

    public Result<Account, BusinessError> updateAccount(UUID accountId, AccountCommand cmd) {
        return accountService.findById(accountId)
                .flatMap(existing -> parse(accountId, cmd))
                .map(accountService::save).ifSuccess(
                        account -> MessageBus.submit(new AccountEvents.Updated(account))
                );
    }

    /** Ids das transações movidas/apagadas (vazio para {@link TransactionPolicy.Block}). */
    public Result<List<UUID>, BusinessError> deleteAccount(UUID accountId, TransactionPolicy policy) {
        return accountService.findById(accountId).flatMap(account -> switch (policy) {
            case TransactionPolicy.Block ignored -> deleteBlock(account);
            case TransactionPolicy.Move(var targetId) -> deleteMove(account, targetId);
            case TransactionPolicy.Purge ignored -> deletePurge(account);
        }).ifSuccess(
                _ -> MessageBus.submit(new AccountEvents.Deleted(accountId))
        );
    }

    private Result<List<UUID>, BusinessError> deleteBlock(Account account) {
        if (!transactionService.findByAccount(account.id()).isEmpty()) {
            return Result.failure(new BusinessError.Conflict("Account has linked transactions and cannot be deleted: " + account.id()));
        }
        cardService.findByAccount(account.id()).forEach(c -> cardService.deleteById(c.id()));
        balanceService.findByAccount(account.id()).forEach(balanceService::deleteById);
        return accountService.deleteById(account.id()).map(ignored -> List.<UUID>of());
    }

    private Result<List<UUID>, BusinessError> deleteMove(Account account, UUID targetId) {
        return accountService.findById(targetId).flatMap(target -> {
            if (target.id().equals(account.id())) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("Target account must be different from source: " + targetId));
            }
            if (!target.active()) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("Target account is inactive: " + targetId));
            }

            val movedIds = transactionService.findByAccount(account.id()).stream().map(Transaction::id).toList();
            transactionService.reassignAccount(account.id(), target.id());

            cardService.findByAccount(account.id()).forEach(card ->
                    cardService.save(new CreditCard(card.id(), card.last4(), target.id(), card.active())));
            balanceService.findByAccount(account.id()).forEach(balanceService::deleteById);

            return accountService.deleteById(account.id()).map(ignored -> {
                balanceRecalculationService.recalculate(target.id());
                return movedIds;
            });
        });
    }

    private Result<List<UUID>, BusinessError> deletePurge(Account account) {
        val ids = transactionService.findByAccount(account.id()).stream().map(Transaction::id).toList();
        ids.forEach(transactionService::deleteById);
        cardService.findByAccount(account.id()).forEach(c -> cardService.deleteById(c.id()));
        balanceService.findByAccount(account.id()).forEach(balanceService::deleteById);

        return accountService.deleteById(account.id()).map(ignored -> ids);
    }

    private Result<Account, BusinessError> parse(UUID accountId, AccountCommand cmd) {
        val typeName = Strings.upper(cmd.type());
        val valid = Arrays.stream(Account.Type.values()).anyMatch(t -> t.name().equals(typeName));
        if (!valid) return Result.failure(new BusinessError.Validation("Unknown account type: " + cmd.type()));
        return Result.success(new Account(accountId, cmd.name(), Account.Type.valueOf(typeName), cmd.active(),
                cmd.creditLimit(), cmd.overdraftLimit(), cmd.closingDay(), cmd.dueDay(), null, null));
    }
}

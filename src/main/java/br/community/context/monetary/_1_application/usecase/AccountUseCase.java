package br.community.context.monetary._1_application.usecase;

import br.commons.Result;
import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.CreditCard;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.monetary._1_application.command.TransactionPolicy;
import br.community.context.monetary._1_application.service.*;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class AccountUseCase {

    private final AccountService accountService;
    private final BalanceService balanceService;
    private final TransactionService transactionService;
    private final CardService cardService;
    private final BalanceRecalculationService balanceRecalculationService;

    public Result<List<Account>, DomainError> listAccounts() {
        return Result.success(accountService.findAll());
    }

    public Result<Account, DomainError> findAccount(UUID id) {
        return accountService.findById(id);
    }

    public Result<MonthlyBalance, DomainError> getMonthlyBalance(UUID accountId, YearMonth period) {
        return accountService.findById(accountId)
                .flatMap(ignored -> balanceService.findByAccountAndPeriod(accountId, period));
    }

    public Result<List<MonthlyBalance>, DomainError> getYearBalances(UUID accountId, int year) {
        return accountService.findById(accountId)
                .map(ignored -> balanceService.findByAccountAndYear(accountId, year));
    }

    public Result<Account, DomainError> createAccount(AccountCommand cmd) {
        return parse(UUID.randomUUID(), cmd).map(accountService::save);
    }

    public Result<Account, DomainError> updateAccount(UUID accountId, AccountCommand cmd) {
        return accountService.findById(accountId)
                .flatMap(existing -> parse(accountId, cmd))
                .map(accountService::save);
    }

    /** Ids das transações movidas/apagadas (vazio para {@link TransactionPolicy.Block}). */
    public Result<List<UUID>, DomainError> deleteAccount(UUID accountId, TransactionPolicy policy) {
        return accountService.findById(accountId).flatMap(account -> switch (policy) {
            case TransactionPolicy.Block ignored -> deleteBlock(account);
            case TransactionPolicy.Move(var targetId) -> deleteMove(account, targetId);
            case TransactionPolicy.Purge ignored -> deletePurge(account);
        });
    }

    private Result<List<UUID>, DomainError> deleteBlock(Account account) {
        if (!transactionService.findByAccount(account.id()).isEmpty()) {
            return Result.failure(new DomainError.Conflict("Account has linked transactions and cannot be deleted: " + account.id()));
        }
        cardService.findByAccount(account.id()).forEach(c -> cardService.deleteById(c.id()));
        balanceService.findByAccount(account.id()).forEach(b -> balanceService.deleteById(b.id()));
        return accountService.deleteById(account.id()).map(ignored -> List.<UUID>of());
    }

    private Result<List<UUID>, DomainError> deleteMove(Account account, UUID targetId) {
        return accountService.findById(targetId).flatMap(target -> {
            if (target.id().equals(account.id())) {
                return Result.<List<UUID>>failure(
                        new DomainError.BusinessRule("Target account must be different from source: " + targetId));
            }
            if (!target.active()) {
                return Result.<List<UUID>>failure(
                        new DomainError.BusinessRule("Target account is inactive: " + targetId));
            }

            val movedIds = transactionService.findByAccount(account.id()).stream().map(Transaction::id).toList();
            transactionService.reassignAccount(account.id(), target.id());

            cardService.findByAccount(account.id()).forEach(card ->
                    cardService.save(new CreditCard(card.id(), card.last4(), target.id(), card.active())));
            balanceService.findByAccount(account.id()).forEach(b -> balanceService.deleteById(b.id()));

            return accountService.deleteById(account.id()).map(ignored -> {
                balanceRecalculationService.recalculate(target.id());
                return movedIds;
            });
        });
    }

    private Result<List<UUID>, DomainError> deletePurge(Account account) {
        val ids = transactionService.findByAccount(account.id()).stream().map(Transaction::id).toList();
        ids.forEach(transactionService::deleteById);
        cardService.findByAccount(account.id()).forEach(c -> cardService.deleteById(c.id()));
        balanceService.findByAccount(account.id()).forEach(b -> balanceService.deleteById(b.id()));

        return accountService.deleteById(account.id()).map(ignored -> ids);
    }

    private Result<Account, DomainError> parse(UUID accountId, AccountCommand cmd) {
        val typeName = Strings.upper(cmd.type());
        val valid = Arrays.stream(Account.Type.values()).anyMatch(t -> t.name().equals(typeName));
        if (!valid) return Result.failure(new DomainError.Validation("Unknown account type: " + cmd.type()));
        return Result.success(new Account(accountId, cmd.name(), Account.Type.valueOf(typeName), cmd.active(),
                cmd.creditLimit(), cmd.overdraftLimit(), cmd.closingDay(), cmd.dueDay(), null, null));
    }
}

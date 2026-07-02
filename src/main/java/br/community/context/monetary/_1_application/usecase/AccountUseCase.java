package br.community.context.monetary._1_application.usecase;

import br.commons.MessageBus;
import br.commons.Result;
import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.event.AccountEvents;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceService;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class AccountUseCase {

    private final AccountService accountService;
    private final BalanceService balanceService;

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
        return parse(UUID.randomUUID(), cmd).map(account -> {
            val created = accountService.save(account);
            MessageBus.submit(new AccountEvents.Created(created));
            return created;
        });
    }

    public Result<Account, DomainError> updateAccount(UUID accountId, AccountCommand cmd) {
        return accountService.findById(accountId)
                .flatMap(existing -> parse(accountId, cmd))
                .map(account -> {
                    val updated = accountService.save(account);
                    MessageBus.submit(new AccountEvents.Updated(updated));
                    return updated;
                });
    }

    public Result<Void, DomainError> deleteAccount(UUID accountId) {
        return accountService.deleteById(accountId)
                .ifSuccess(ignored -> MessageBus.submit(new AccountEvents.Deleted(accountId)));
    }

    private Result<Account, DomainError> parse(UUID accountId, AccountCommand cmd) {
        val typeName = Strings.upper(cmd.type());
        val valid = Arrays.stream(Account.Type.values()).anyMatch(t -> t.name().equals(typeName));
        if (!valid) return Result.failure(new DomainError.Validation("Unknown account type: " + cmd.type()));
        return Result.success(new Account(accountId, cmd.name(), Account.Type.valueOf(typeName), cmd.active()));
    }
}

package br.community.context.monetary;

import br.commons.MessageBus;
import br.commons.Result;
import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.event.MonetaryEvent;
import br.community.context.monetary._0_domain.model.AccountType;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.monetary._1_application.command.CreditCardCommand;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceService;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class AccountUseCase {

    private final AccountService accountService;
    private final BalanceService balanceService;

    public Result<List<MonetaryAccount>, DomainError> listAccounts() {
        return Result.success(accountService.findAll());
    }

    public Result<MonetaryAccount, DomainError> findAccount(UUID id) {
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

    public Result<MonetaryAccount, DomainError> createAccount(AccountCommand cmd) {
        if (AccountType.CREDIT_CARD.name().equalsIgnoreCase(cmd.type())) {
            if (cmd.linkedAccountId() == null) {
                return Result.failure(new DomainError.BusinessRule("Credit card must be linked to a checking account"));
            }
            val validation = validateCreditCardAccount(cmd.linkedAccountId(), cmd.color());
            if (validation instanceof Result.Failure<Void, DomainError>(var error)) return Result.failure(error);
        }
        val created = accountService.save(parse(UUID.randomUUID(), cmd));
        MessageBus.submit(new MonetaryEvent.AccountCreated(created));
        return Result.success(created);
    }

    public Result<MonetaryAccount, DomainError> updateAccount(UUID accountId, AccountCommand cmd) {
        return accountService.findById(accountId)
                .flatMap(existing -> {
                    if (AccountType.CREDIT_CARD.name().equalsIgnoreCase(cmd.type())) {
                        if (cmd.linkedAccountId() == null) {
                            return Result.failure(new DomainError.BusinessRule("Credit card must be linked to a checking account"));
                        }
                        val validation = validateCreditCardAccount(cmd.linkedAccountId(), cmd.color());
                        if (validation instanceof Result.Failure<Void, DomainError>(var error)) return Result.failure(error);
                    }
                    val updated = accountService.save(parse(accountId, cmd));
                    MessageBus.submit(new MonetaryEvent.AccountUpdated(updated));
                    return Result.success(updated);
                });
    }



    public Result<Void, DomainError> deleteAccount(UUID accountId) {
        return accountService.deleteById(accountId)
                .ifSuccess(ignored -> MessageBus.submit(new MonetaryEvent.AccountDeleted(accountId)));
    }

    private MonetaryAccount parse(UUID accountId, AccountCommand cmd) {
        val account = new MonetaryAccount(accountId, cmd.name(), AccountType.valueOf(Strings.upper(cmd.type())),
                cmd.balance(), cmd.color(), cmd.active(), cmd.linkedAccountId());

        if (cmd.additionalInfo() != null) account.additionalInfo().putAll(cmd.additionalInfo());
        return account;
    }

    // ── Credit card operations ─────────────────────────────────────

    public Result<List<MonetaryAccount>, DomainError> listCreditCards() {
        return Result.success(accountService.findCreditCards());
    }

    public Result<List<MonetaryAccount>, DomainError> listCreditCardsByAccount(UUID accountId) {
        return Result.success(accountService.findCreditCardsByAccount(accountId));
    }

    public Result<MonetaryAccount, DomainError> createCreditCard(CreditCardCommand cmd) {
        return validateCreditCardAccount(cmd.accountId(), cmd.color())
                .map(ignored -> accountService.save(toCreditCardEntity(UUID.randomUUID(), cmd)));
    }

    public Result<MonetaryAccount, DomainError> updateCreditCard(UUID accountId, CreditCardCommand cmd) {
        return accountService.findById(accountId)
                .flatMap(existing -> validateCreditCardAccount(cmd.accountId(), cmd.color())
                        .map(ignored -> accountService.save(toCreditCardEntity(accountId, cmd))));
    }

    public Result<Void, DomainError> deleteCreditCard(UUID accountId) {
        return accountService.deleteById(accountId);
    }

    private Result<Void, DomainError> validateCreditCardAccount(UUID accountId, String color) {
        return findAccount(accountId)
                .flatMap(account -> {
                    if (!AccountType.CHECKING.equals(account.type())) {
                        return Result.failure(new DomainError.BusinessRule("Credit card must be linked to a checking account"));
                    }
                    if (!account.color().equalsIgnoreCase(color)) {
                        return Result.failure(new DomainError.BusinessRule("Credit card color must match the linked account color"));
                    }
                    return Result.success();
                });
    }

    private MonetaryAccount toCreditCardEntity(UUID id, CreditCardCommand cmd) {
        val account = new MonetaryAccount(id, cmd.name(), AccountType.CREDIT_CARD, cmd.limit(), cmd.color(), cmd.active(), cmd.accountId());
        account.additionalInfo().put("last4", cmd.last4());
        account.additionalInfo().put("dueDay", cmd.dueDay());
        account.additionalInfo().put("closingDay", cmd.closingDay());
        return account;
    }

}

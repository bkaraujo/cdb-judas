package br.cdb.feature.f002._1_application.usecase;

import br.cdb.feature.f000._0_domain.TransactionPolicy;
import br.cdb.feature.f002._0_domain.event.AccountEvents;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._1_application.command.AccountCommand;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@NullMarked
public class AccountUseCase {

    private final AccountService service = Registry.tryGet(AccountService.class);
    private final BalanceService balanceService = Registry.tryGet(BalanceService.class);
    private final CreditCardService creditCardService = Registry.tryGet(CreditCardService.class);
    private final TransactionService transactionService = Registry.tryGet(TransactionService.class);

    public Result<List<Account>, BusinessError> listAccounts(String personId) {
        return Result.success(service.findAllByPerson(personId));
    }

    public Result<Account, BusinessError> findAccount(UUID id, String personId) {
        return service.findByIdAndPerson(id, personId);
    }

    public Result<Balance, BusinessError> getMonthlyBalance(UUID accountId, YearMonth period) {
        return service.findById(accountId)
                .flatMap(ignored -> balanceService.findByAccountAndPeriod(accountId, period));
    }

    public Result<List<Balance>, BusinessError> getYearBalances(UUID accountId, int year) {
        return service.findById(accountId)
                .map(ignored -> balanceService.findByAccountAndYear(accountId, year));
    }

    public Result<Account, BusinessError> upsert(AccountCommand.Upsert cmd) {
        return switch (cmd) {
            case AccountCommand.Create(var name, var type, var active, var creditLimit, var overdraftLimit, var closingDay, var dueDay) ->
                    parse(UUID.randomUUID(), name, type, active, creditLimit, overdraftLimit, closingDay, dueDay)
                            .map(service::save)
                            .ifSuccess(account -> MessageBus.submit(new AccountEvents.Created(account)));
            case AccountCommand.Update(var id, var name, var type, var active, var creditLimit, var overdraftLimit, var closingDay, var dueDay) ->
                    service.findById(id)
                            .flatMap(existing -> parse(id, name, type, active, creditLimit, overdraftLimit, closingDay, dueDay))
                            .map(service::save)
                            .ifSuccess(account -> MessageBus.submit(new AccountEvents.Updated(account)));
        };
    }

    /** Ids das transações movidas/apagadas (vazio para {@link TransactionPolicy.Block}). */
    public Result<List<UUID>, BusinessError> delete(AccountCommand.Delete command) {
        return service.findById(command.id()).flatMap(account -> switch (command.policy()) {
            case TransactionPolicy.Block ignored -> deleteBlock(account);
            case TransactionPolicy.Move(var targetId) -> deleteMove(account, targetId);
            case TransactionPolicy.Purge ignored -> deletePurge(account);
        }).ifSuccess(
                _ -> MessageBus.submit(new AccountEvents.Deleted(command.id()))
        );
    }

    private Result<List<UUID>, BusinessError> deleteBlock(Account account) {
        if (!transactionService.findByAccount(account.id()).isEmpty()) {
            return Result.failure(new BusinessError.Conflict("Account has linked transactions and cannot be deleted: " + account.id()));
        }
        creditCardService.findByAccount(account.id()).forEach(c -> creditCardService.deleteById(c.id()));
        balanceService.findByAccount(account.id()).forEach(balanceService::deleteById);
        return service.deleteById(account.id()).map(ignored -> List.<UUID>of());
    }

    private Result<List<UUID>, BusinessError> deleteMove(Account account, UUID targetId) {
        return service.findById(targetId).flatMap(target -> {
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
                                                  @Nullable BigDecimal creditLimit, @Nullable BigDecimal overdraftLimit,
                                                  @Nullable Integer closingDay, @Nullable Integer dueDay) {
        val typeName = Strings.upper(type);
        val valid = Arrays.stream(Account.Type.values()).anyMatch(t -> t.name().equals(typeName));
        if (!valid) return Result.failure(new BusinessError.Validation("Unknown account type: " + type));
        return Result.success(new Account(accountId, name, Account.Type.valueOf(typeName), active,
                creditLimit, overdraftLimit, closingDay, dueDay, null, null));
    }
}

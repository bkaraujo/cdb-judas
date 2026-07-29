package br.cdb.feature.f003._1_application.usecase;

import br.cdb.feature.f000._0_domain.TransactionPolicy;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003._0_domain.event.CreditCardEvents;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.command.CreditCardCommand;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@NullMarked
public class CreditCardUseCase {

    private static final Pattern LAST4 = Pattern.compile("\\d{4}");

    private final CreditCardService service = Registry.tryGet(CreditCardService.class);
    private final AccountService accountService = Registry.tryGet(AccountService.class);
    private final BalanceService balanceService = Registry.tryGet(BalanceService.class);
    private final TransactionService transactionService = Registry.tryGet(TransactionService.class);

    public Result<List<CreditCard>, BusinessError> list(String personId) {
        return Result.success(service.findAllByPerson(personId));
    }

    public Result<List<CreditCard>, BusinessError> list(UUID accountId, String personId) {
        return accountService.findByIdAndPerson(accountId, personId)
                .map(ignored -> service.findByAccountAndPerson(accountId, personId));
    }

    public Result<CreditCard, BusinessError> upsert(CreditCardCommand.Upsert cmd) {
        return switch (cmd) {
            case CreditCardCommand.Create(var accountId, var last4) -> createCard(accountId, last4);
            case CreditCardCommand.Update(var id, var active) -> setActive(id, active);
        };
    }

    private Result<CreditCard, BusinessError> createCard(UUID accountId, String last4) {
        if (!LAST4.matcher(last4).matches()) {
            return Result.failure(new BusinessError.Validation("last4 must be exactly 4 digits"));
        }

        return accountService
                .findById(accountId)
                .flatMap(account -> createCardFor(last4, account))
                .ifSuccess(account -> MessageBus.submit(new CreditCardEvents.Created(account)));
    }

    private Result<CreditCard, BusinessError> createCardFor(String last4, Account account) {
        if (!account.active()) {
            return Result.failure(new BusinessError.BusinessRule("Account is inactive: " + account.id()));
        }

        val duplicate = service.findByAccount(account.id()).stream()
                .anyMatch(c -> c.last4().equals(last4));
        if (duplicate) {
            return Result.failure(new BusinessError.Conflict("CreditCard already registered for this account: " + last4));
        }

        val saved = service.save(new CreditCard(UUID.randomUUID(), last4, account.id(), true));
        return Result.success(saved);
    }

    /** Ids das transações movidas/apagadas (vazio para {@link TransactionPolicy.Block}). */
    public Result<List<UUID>, BusinessError> delete(CreditCardCommand.Delete command) {
        return service.findById(command.id()).flatMap(card -> switch (command.policy()) {
            case TransactionPolicy.Block ignored -> deleteBlock(card);
            case TransactionPolicy.Move(var targetId) -> deleteMove(card, targetId);
            case TransactionPolicy.Purge ignored -> deletePurge(card);
        })
                .ifSuccess(_ -> MessageBus.submit(new CreditCardEvents.Deleted(command.id())));
    }

    private Result<List<UUID>, BusinessError> deleteBlock(CreditCard creditCard) {
        if (!transactionService.findByCard(creditCard.id()).isEmpty()) {
            return Result.failure(new BusinessError.Conflict("CreditCard has linked transactions and cannot be deleted: " + creditCard.id()));
        }
        service.deleteById(creditCard.id());
        return Result.success(List.of());
    }

    private Result<List<UUID>, BusinessError> deleteMove(CreditCard creditCard, UUID targetId) {
        return service.findById(targetId).flatMap(target -> {
            if (target.id().equals(creditCard.id())) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("Target creditCard must be different from source: " + targetId));
            }
            if (!target.accountId().equals(creditCard.accountId())) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("Target creditCard must belong to the same account: " + targetId));
            }
            if (!target.active()) {
                return Result.<List<UUID>>failure(
                        new BusinessError.BusinessRule("Target creditCard is inactive: " + targetId));
            }

            val movedIds = transactionService.findByCard(creditCard.id()).stream().map(Transaction::id).toList();
            transactionService.reassignCard(creditCard.id(), target.id());
            service.deleteById(creditCard.id());
            return Result.success(movedIds);
        });
    }

    private Result<List<UUID>, BusinessError> deletePurge(CreditCard creditCard) {
        val ids = transactionService.findByCard(creditCard.id()).stream().map(Transaction::id).toList();
        ids.forEach(transactionService::deleteById);
        service.deleteById(creditCard.id());

        return accountService.findById(creditCard.accountId()).map(account -> {
            balanceService.recalculate(account.id());
            return ids;
        });
    }

    private Result<CreditCard, BusinessError> setActive(UUID id, boolean active) {
        return service.findById(id)
                .map(card -> service.save(new CreditCard(card.id(), card.last4(), card.accountId(), active)))
                .ifSuccess(account -> MessageBus.submit(new CreditCardEvents.Updated(account)));
    }
}

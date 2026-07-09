package br.community.context.monetary._1_application.usecase;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.CreditCard;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.CardCommand;
import br.community.context.monetary._1_application.command.TransactionPolicy;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceRecalculationService;
import br.community.context.monetary._1_application.service.CardService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@NullMarked
@RequiredArgsConstructor
public class CardUseCase {

    private static final Pattern LAST4 = Pattern.compile("\\d{4}");

    private final CardService cardService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final BalanceRecalculationService balanceRecalculationService;

    public Result<List<CreditCard>, DomainError> listCards() {
        return Result.success(cardService.findAll());
    }

    public Result<List<CreditCard>, DomainError> listCardsByAccount(UUID accountId) {
        return accountService.findById(accountId).map(ignored -> cardService.findByAccount(accountId));
    }

    public Result<CreditCard, DomainError> createCard(CardCommand cmd) {
        if (!LAST4.matcher(cmd.last4()).matches()) {
            return Result.failure(new DomainError.Validation("last4 must be exactly 4 digits"));
        }
        return accountService.findById(cmd.accountId()).flatMap(account -> createCardFor(cmd, account));
    }

    private Result<CreditCard, DomainError> createCardFor(CardCommand cmd, Account account) {
        if (!account.active()) {
            return Result.failure(new DomainError.BusinessRule("Account is inactive: " + account.id()));
        }

        val duplicate = cardService.findByAccount(account.id()).stream()
                .anyMatch(c -> c.last4().equals(cmd.last4()));
        if (duplicate) {
            return Result.failure(new DomainError.Conflict("CreditCard already registered for this account: " + cmd.last4()));
        }

        val saved = cardService.save(new CreditCard(UUID.randomUUID(), cmd.last4(), account.id(), true));
        return Result.success(saved);
    }

    /** Ids das transações movidas/apagadas (vazio para {@link TransactionPolicy.Block}). */
    public Result<List<UUID>, DomainError> deleteCard(UUID id, TransactionPolicy policy) {
        return cardService.findById(id).flatMap(card -> switch (policy) {
            case TransactionPolicy.Block ignored -> deleteBlock(card);
            case TransactionPolicy.Move(var targetId) -> deleteMove(card, targetId);
            case TransactionPolicy.Purge ignored -> deletePurge(card);
        });
    }

    private Result<List<UUID>, DomainError> deleteBlock(CreditCard creditCard) {
        if (!transactionService.findByCard(creditCard.id()).isEmpty()) {
            return Result.failure(new DomainError.Conflict("CreditCard has linked transactions and cannot be deleted: " + creditCard.id()));
        }
        cardService.deleteById(creditCard.id());
        return Result.success(List.of());
    }

    private Result<List<UUID>, DomainError> deleteMove(CreditCard creditCard, UUID targetId) {
        return cardService.findById(targetId).flatMap(target -> {
            if (target.id().equals(creditCard.id())) {
                return Result.<List<UUID>>failure(
                        new DomainError.BusinessRule("Target creditCard must be different from source: " + targetId));
            }
            if (!target.accountId().equals(creditCard.accountId())) {
                return Result.<List<UUID>>failure(
                        new DomainError.BusinessRule("Target creditCard must belong to the same account: " + targetId));
            }
            if (!target.active()) {
                return Result.<List<UUID>>failure(
                        new DomainError.BusinessRule("Target creditCard is inactive: " + targetId));
            }

            val movedIds = transactionService.findByCard(creditCard.id()).stream().map(Transaction::id).toList();
            transactionService.reassignCard(creditCard.id(), target.id());
            cardService.deleteById(creditCard.id());
            return Result.success(movedIds);
        });
    }

    private Result<List<UUID>, DomainError> deletePurge(CreditCard creditCard) {
        val ids = transactionService.findByCard(creditCard.id()).stream().map(Transaction::id).toList();
        ids.forEach(transactionService::deleteById);
        cardService.deleteById(creditCard.id());

        return accountService.findById(creditCard.accountId()).map(account -> {
            balanceRecalculationService.recalculate(account.id());
            return ids;
        });
    }

    public Result<CreditCard, DomainError> setActive(UUID id, boolean active) {
        return cardService.findById(id)
                .map(card -> cardService.save(new CreditCard(card.id(), card.last4(), card.accountId(), active)));
    }
}

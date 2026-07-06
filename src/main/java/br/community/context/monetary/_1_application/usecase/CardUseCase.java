package br.community.context.monetary._1_application.usecase;

import br.commons.MessageBus;
import br.commons.Result;
import br.community.context.monetary._0_domain.event.AccountEvents;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._1_application.command.CardCommand;
import br.community.context.monetary._1_application.service.AccountService;
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

    public Result<List<Card>, DomainError> listCards() {
        return Result.success(cardService.findAll());
    }

    public Result<List<Card>, DomainError> listCardsByAccount(UUID accountId) {
        return accountService.findById(accountId).map(ignored -> cardService.findByAccount(accountId));
    }

    public Result<Card, DomainError> createCard(CardCommand cmd) {
        if (!LAST4.matcher(cmd.last4()).matches()) {
            return Result.failure(new DomainError.Validation("last4 must be exactly 4 digits"));
        }
        return accountService.findById(cmd.accountId()).flatMap(account -> createCardFor(cmd, account));
    }

    private Result<Card, DomainError> createCardFor(CardCommand cmd, Account account) {
        if (!account.active()) {
            return Result.failure(new DomainError.BusinessRule("Account is inactive: " + account.id()));
        }

        val duplicate = cardService.findByAccount(account.id()).stream()
                .anyMatch(c -> c.last4().equals(cmd.last4()));
        if (duplicate) {
            return Result.failure(new DomainError.Conflict("Card already registered for this account: " + cmd.last4()));
        }

        val saved = cardService.save(new Card(UUID.randomUUID(), cmd.last4(), account.id(), true));
        MessageBus.submit(new AccountEvents.Updated(account));
        return Result.success(saved);
    }

    public Result<Void, DomainError> deleteCard(UUID id) {
        return cardService.findById(id).flatMap(card -> {
            if (!transactionService.findByCard(id).isEmpty()) {
                return Result.<Void>failure(new DomainError.Conflict("Card has linked transactions and cannot be deleted: " + id));
            }
            cardService.deleteById(id);
            accountService.findById(card.accountId())
                    .ifSuccess(account -> MessageBus.submit(new AccountEvents.Updated(account)));
            return Result.success();
        });
    }

    public Result<Card, DomainError> setActive(UUID id, boolean active) {
        return cardService.findById(id).map(card -> {
            val saved = cardService.save(new Card(card.id(), card.last4(), card.accountId(), active));
            accountService.findById(saved.accountId())
                    .ifSuccess(account -> MessageBus.submit(new AccountEvents.Updated(account)));
            return saved;
        });
    }
}

package br.community.context.monetary._1_application.usecase;

import br.commons.MessageBus;
import br.commons.Result;
import br.community.context.monetary._0_domain.event.AccountEvents;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.AccountLimit;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._1_application.command.AccountLimitCommand;
import br.community.context.monetary._1_application.command.CardCommand;
import br.community.context.monetary._1_application.service.AccountLimitService;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.CardService;
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
    private final AccountLimitService accountLimitService;

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
        return cardService.findById(id).map(card -> {
            cardService.deleteById(id);
            accountService.findById(card.accountId())
                    .ifSuccess(account -> MessageBus.submit(new AccountEvents.Updated(account)));
            return null;
        });
    }

    public Result<AccountLimit, DomainError> getAccountLimit(UUID accountId) {
        return accountService.findById(accountId).map(ignored ->
                accountLimitService.findByAccount(accountId)
                        .orElseGet(() -> new AccountLimit(accountId, null, null, null, null)));
    }

    public Result<AccountLimit, DomainError> setAccountLimit(UUID accountId, AccountLimitCommand cmd) {
        return accountService.findById(accountId).map(account -> {
            val limit = new AccountLimit(accountId, cmd.creditLimit(), cmd.overdraftLimit(), cmd.closingDay(), cmd.dueDay());
            val result = limit.isEmpty() ? emptyLimit(accountId) : accountLimitService.save(limit);
            MessageBus.submit(new AccountEvents.Updated(account));
            return result;
        });
    }

    public Result<List<AccountLimit>, DomainError> listAccountLimits() {
        return Result.success(accountLimitService.findAll());
    }

    private AccountLimit emptyLimit(UUID accountId) {
        accountLimitService.deleteByAccount(accountId);
        return new AccountLimit(accountId, null, null, null, null);
    }
}

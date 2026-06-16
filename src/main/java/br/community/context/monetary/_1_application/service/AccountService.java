package br.community.context.monetary._1_application.service;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<MonetaryAccount> findAll() {
        return accountRepository.findAll();
    }

    public Result<MonetaryAccount, DomainError> findById(UUID accountId) {
        return accountRepository.findById(accountId)
                .<Result<MonetaryAccount, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Account not found: " + accountId)));
    }

    public MonetaryAccount save(MonetaryAccount account) {
        return accountRepository.save(account);
    }

    public Result<Void, DomainError> deleteById(UUID accountId) {
        return findById(accountId).map(existing -> {
            accountRepository.deleteById(accountId);
            return null;
        });
    }

    public List<MonetaryAccount> findCreditCards() {
        return accountRepository.findAll().stream()
                .filter(a -> a.type() == MonetaryAccount.Type.CREDIT_CARD)
                .toList();
    }

    public List<MonetaryAccount> findCreditCardsByAccount(UUID accountId) {
        return findCreditCards().stream()
                .filter(c -> accountId.equals(c.linkedAccountId()))
                .toList();
    }
}

package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.repository.AccountRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Result<Account, BusinessError> findById(UUID accountId) {
        return accountRepository.findById(accountId)
                .<Result<Account, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Account not found: " + accountId)));
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    public Result<Void, BusinessError> deleteById(UUID accountId) {
        return findById(accountId).flatMap(existing -> {
            accountRepository.deleteById(accountId);
            return Result.success();
        });
    }
}

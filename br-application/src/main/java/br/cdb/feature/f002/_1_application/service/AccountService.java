package br.cdb.feature.f002._1_application.service;

import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public class AccountService {

    private final AccountRepository repository = Context.get(AccountRepository.class);

    public List<Account> findAll() {
        return repository.findAll();
    }

    public Result<Account, BusinessError> findById(UUID accountId) {
        return repository.findById(accountId)
                .<Result<Account, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("account.notFound", accountId)));
    }

    public List<Account> findAllByPerson(String personId) {
        return repository.findAllByPerson(personId);
    }

    public Result<Account, BusinessError> findByIdAndPerson(UUID accountId, String personId) {
        return repository.findByIdAndPerson(accountId, personId)
                .<Result<Account, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("account.notFound", accountId)));
    }

    public Account save(Account account) {
        return repository.save(account);
    }

    public Result<Void, BusinessError> deleteById(UUID accountId) {
        return findById(accountId).flatMap(_ -> {
            repository.deleteById(accountId);
            return Result.success();
        });
    }
}

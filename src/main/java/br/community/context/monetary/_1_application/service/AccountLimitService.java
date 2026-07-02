package br.community.context.monetary._1_application.service;

import br.community.context.monetary._0_domain.model.AccountLimit;
import br.community.context.monetary._0_domain.repository.AccountLimitRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class AccountLimitService {

    private final AccountLimitRepository accountLimitRepository;

    public List<AccountLimit> findAll() {
        return accountLimitRepository.findAll();
    }

    public Optional<AccountLimit> findByAccount(UUID accountId) {
        return accountLimitRepository.findById(accountId);
    }

    public AccountLimit save(AccountLimit limit) {
        return accountLimitRepository.save(limit);
    }

    public void deleteByAccount(UUID accountId) {
        accountLimitRepository.deleteById(accountId);
    }
}

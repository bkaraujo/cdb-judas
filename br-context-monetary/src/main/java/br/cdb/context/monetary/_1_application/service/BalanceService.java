package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.AccountBalance;
import br.cdb.context.monetary._0_domain.repository.BalanceRepository;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NullMarked
public class BalanceService {

    private final BalanceRepository balanceRepository = Registry.get(BalanceRepository.class);

    public List<AccountBalance> findByAccount(UUID accountId) {
        return balanceRepository.findByAccount(accountId);
    }

    public Result<AccountBalance, BusinessError> findByAccountAndPeriod(UUID accountId, YearMonth period) {
        return balanceRepository.findByAccount(accountId).stream()
                .filter(b -> b.period().equals(period))
                .findFirst()
                .<Result<AccountBalance, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Balance not found for period: " + period)));
    }

    public List<AccountBalance> findByAccountAndYear(UUID accountId, int year) {
        return balanceRepository.findByAccount(accountId).stream()
                .filter(b -> b.period().getYear() == year)
                .sorted(Comparator.comparing(AccountBalance::period))
                .toList();
    }

    public AccountBalance save(AccountBalance balance) {
        return balanceRepository.save(balance);
    }

    public void deleteById(AccountBalance balance) {
        balanceRepository.delete(balance.accountId(), balance.period());
    }
}

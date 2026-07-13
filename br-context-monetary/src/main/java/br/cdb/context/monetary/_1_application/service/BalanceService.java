package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.MonthlyBalance;
import br.cdb.context.monetary._0_domain.repository.BalanceRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;

    public List<MonthlyBalance> findByAccount(UUID accountId) {
        return balanceRepository.findByAccount(accountId);
    }

    public Result<MonthlyBalance, BusinessError> findByAccountAndPeriod(UUID accountId, YearMonth period) {
        return balanceRepository.findByAccount(accountId).stream()
                .filter(b -> b.period().equals(period))
                .findFirst()
                .<Result<MonthlyBalance, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Balance not found for period: " + period)));
    }

    public List<MonthlyBalance> findByAccountAndYear(UUID accountId, int year) {
        return balanceRepository.findByAccount(accountId).stream()
                .filter(b -> b.period().getYear() == year)
                .sorted(Comparator.comparing(MonthlyBalance::period))
                .toList();
    }

    public MonthlyBalance save(MonthlyBalance balance) {
        return balanceRepository.save(balance);
    }

    public void deleteById(UUID id) {
        balanceRepository.deleteById(id);
    }
}

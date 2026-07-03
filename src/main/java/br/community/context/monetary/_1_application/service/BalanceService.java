package br.community.context.monetary._1_application.service;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import br.community.context.shared._0_domain.model.DomainError;
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

    public Result<MonthlyBalance, DomainError> findByAccountAndPeriod(UUID accountId, YearMonth period) {
        return balanceRepository.findByAccount(accountId).stream()
                .filter(b -> b.period().equals(period))
                .findFirst()
                .<Result<MonthlyBalance, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Balance not found for period: " + period)));
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

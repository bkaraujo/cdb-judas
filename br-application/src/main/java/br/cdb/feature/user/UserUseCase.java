package br.cdb.feature.user;

import br.cdb.feature.dashboard.DashboardService;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * O que restou do antigo god-object da fatia {@code feature.user}, após a reestruturação fNNN
 * (.claude/refactor.md) extrair accounts/cards/balance (f002), transactions/transfer (f005),
 * tags (f008), categories (f007) e importação de extrato (f006) para seus próprios use cases: só
 * dashboard (candidata a f009) continua aqui.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserUseCase {

    private final DashboardService dashboardService;

    public Result<DashboardService.MonthlyResult, BusinessError> monthlyResult(int month, int year) {
        return dashboardService.getMonthlyResult(month, year);
    }
}

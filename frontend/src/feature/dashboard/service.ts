import type {TransactionResponse} from '@/api/overrides.ts';
import type {Period} from '@/core/kernel/_0_domain/period.ts';
import type {DashboardRepository, MonthlyResult} from '@/feature/dashboard/repository.ts';

export interface DashboardService {
  monthlyResult(period: Period): Promise<MonthlyResult | null>;
  recentTransactions(limit?: number): Promise<TransactionResponse[] | null>;
}

export interface DashboardServiceDeps {
  repo: DashboardRepository;
}

export function createDashboardService(deps: DashboardServiceDeps): DashboardService {
  const repo = deps.repo;
  return {
    monthlyResult: (period) => repo.getMonthlyResult(period.month, period.year),
    recentTransactions: (limit) => repo.getRecentTransactions(limit),
  };
}

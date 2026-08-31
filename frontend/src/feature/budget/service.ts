import type {Period} from '@/core/kernel/_0_domain/period.ts';
import * as BudgetDomain from '@/feature/budget/domain.ts';
import type {BudgetRepository} from '@/feature/budget/repository.ts';
import type {BudgetCreateRequest, BudgetResponse, BudgetUpdateRequest} from '@/feature/budget/types.ts';

export interface BudgetSummary {
  total: number;
  overspending: number;
}

export interface BudgetService {
  loadPeriod(period: Period): Promise<BudgetResponse[] | null>;
  save(id: string, data: BudgetUpdateRequest): Promise<BudgetResponse | null>;
  create(data: BudgetCreateRequest): Promise<BudgetResponse | null>;
  remove(id: string): Promise<void | null>;
  summary(items: readonly BudgetResponse[] | null | undefined): BudgetSummary;
}

export interface BudgetServiceDeps {
  repo: BudgetRepository;
}

export function createBudgetService(deps: BudgetServiceDeps): BudgetService {
  const repo = deps.repo;
  return {
    loadPeriod: (period) => repo.list(period.month, period.year),
    save: (id, data) => repo.update(id, data),
    create: (data) => repo.create(data),
    remove: (id) => repo.remove(id),
    summary: (items) => ({
      total: (items || []).length,
      overspending: BudgetDomain.overspendCount(items),
    }),
  };
}

/** Contrato público da fatia budget (equivalente ao FNNNApi do backend). Único arquivo que outra
 * fatia pode referenciar. Consumidor: dashboard (painel de metas de despesa). */
import type { Period } from '../../core/kernel/_0_domain/period.ts';
import * as BudgetDomain from './domain.ts';
import type { BudgetService } from './service.ts';
import type { BudgetResponse } from './types.ts';

export interface BudgetApi {
  loadPeriod(period: Period): Promise<BudgetResponse[] | null>;
  consumptionPct(spent: number, budgeted: number): number;
  isOverBudget(spent: number, budgeted: number): boolean;
}

export function createBudgetApi(service: BudgetService): BudgetApi {
  return {
    loadPeriod: (period) => service.loadPeriod(period),
    consumptionPct: (spent, budgeted) => BudgetDomain.consumptionPct(spent, budgeted),
    isOverBudget: (spent, budgeted) => BudgetDomain.isOverBudget(spent, budgeted),
  };
}

/** HTTP adapter for /dashboard + o atalho de últimos lançamentos (mesma rota de transactions,
 * outro parâmetro de query). */
import type { TransactionResponse } from '../../api/overrides.ts';
import type { HttpClient } from '../../core/kernel/_2_infrastructure/secondary/http-client.ts';

export interface MonthlyResult {
  month: number;
  year: number;
  receitas: number;
  despesas: number;
}

export interface DashboardRepository {
  getMonthlyResult(month: number, year: number): Promise<MonthlyResult | null>;
  getRecentTransactions(limit?: number): Promise<TransactionResponse[] | null>;
}

export function createDashboardRepository(http: HttpClient): DashboardRepository {
  return {
    getMonthlyResult: (month, year) => http.get<MonthlyResult>('/dashboard?month=' + month + '&year=' + year),
    getRecentTransactions: (limit) => http.get<TransactionResponse[]>('/accounts/transactions?limit=' + (limit || 5) + '&sort=date,desc'),
  };
}

/** HTTP adapter for /budget. */
import type { HttpClient } from '../../core/kernel/_2_infrastructure/secondary/http-client.ts';
import type { BudgetCreateRequest, BudgetResponse, BudgetUpdateRequest } from './types.ts';

export interface BudgetRepository {
  list(month: number, year: number): Promise<BudgetResponse[] | null>;
  create(data: BudgetCreateRequest): Promise<BudgetResponse | null>;
  update(id: string, data: BudgetUpdateRequest): Promise<BudgetResponse | null>;
  remove(id: string): Promise<void | null>;
}

export function createBudgetRepository(http: HttpClient): BudgetRepository {
  return {
    list: (month, year) => http.get<BudgetResponse[]>('/budget?month=' + month + '&year=' + year),
    create: (data) => http.post<BudgetResponse>('/budget', data),
    update: (id, data) => http.patch<BudgetResponse>('/budget/' + id, data),
    remove: (id) => http.delete('/budget/' + id),
  };
}

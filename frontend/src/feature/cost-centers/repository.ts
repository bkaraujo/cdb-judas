/** Read-only adapter for the global /cost-center catalog (fixed system data; no user namespace,
 * no mutations). */
import type { CostCenter } from '../../api/types.ts';
import type { HttpClient } from '../../core/kernel/_2_infrastructure/secondary/http-client.ts';

export interface CostCenterRepository {
  list(): Promise<CostCenter[] | null>;
}

export function createCostCenterRepository(http: HttpClient): CostCenterRepository {
  return {
    list: () => http.global.get<CostCenter[]>('/cost-center'),
  };
}

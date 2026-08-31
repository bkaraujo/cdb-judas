/** HTTP adapter for /accounts/transaction/rules. */
import type {ImportRule, ImportRuleRequest} from '@/api/types.ts';
import type {HttpClient} from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';

export interface ImportRuleRepository {
  list(): Promise<ImportRule[] | null>;
  create(data: ImportRuleRequest): Promise<ImportRule | null>;
  update(id: string, data: ImportRuleRequest): Promise<ImportRule | null>;
  remove(id: string): Promise<void | null>;
}

export function createImportRuleRepository(http: HttpClient): ImportRuleRepository {
  return {
    list: () => http.get<ImportRule[]>('/accounts/transaction/rules'),
    create: (data) => http.post<ImportRule>('/accounts/transaction/rules', data),
    update: (id, data) => http.patch<ImportRule>('/accounts/transaction/rules/' + id, data),
    remove: (id) => http.delete('/accounts/transaction/rules/' + id),
  };
}

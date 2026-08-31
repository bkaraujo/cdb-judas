/** Contrato público da fatia import-rules (equivalente ao FNNNApi do backend). Único arquivo que
 * outra fatia pode referenciar. Consumidores: transactions (form manual), import-statement
 * (preview de extrato/fatura) — casam a descrição digitada/importada contra as regras cacheadas. */
import type {ImportRule} from '@/api/types.ts';
import type {CacheStore} from '@/core/kernel/_1_application/cache-store.ts';
import * as ImportRuleMatcher from '@/feature/import-rules/domain.ts';
import type {ImportRuleService} from '@/feature/import-rules/service.ts';

export interface ImportRulesApi {
  match(description: string | null | undefined, rules: readonly ImportRule[] | null | undefined): ImportRule | null;
  listCached(): ImportRule[];
  /** Única gravação autorizada em `CacheStore.importRules` fora da própria fatia — quick-create no
   * cabeçalho do preview de import-statement precisa ficar elegível na hora, sem esperar reload. */
  quickCreate(rule: ImportRule): void;
}

export function createImportRulesApi(service: ImportRuleService, cache: CacheStore): ImportRulesApi {
  return {
    match: (description, rules) => ImportRuleMatcher.match(description, rules),
    listCached: () => service.listCached(),
    quickCreate: (rule) => cache.setImportRules([...cache.importRules(), rule]),
  };
}

/** ImportRule (regra de nomenclatura) use cases. */
import type { ImportRule, ImportRuleRequest } from '../../api/types.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import type { CbdChangeDetail } from '../../core/kernel/_1_application/event-bus.ts';
import type { ImportRuleRepository } from './repository.ts';

export interface ImportRuleServiceDeps {
  repo: ImportRuleRepository;
  cache: CacheStore;
}

export interface ImportRuleService {
  list(): Promise<ImportRule[] | null>;
  create(data: ImportRuleRequest): Promise<ImportRule | null>;
  update(id: string, data: ImportRuleRequest): Promise<ImportRule | null>;
  remove(id: string): Promise<void | null>;
  listCached(): ImportRule[];
  findById(id: string | null | undefined): ImportRule | null;
  onChange(cb: (detail: CbdChangeDetail) => void): () => void;
}

export function createImportRuleService(deps: ImportRuleServiceDeps): ImportRuleService {
  return {
    list: () => deps.repo.list(),
    create: (data) => deps.repo.create(data),
    update: (id, data) => deps.repo.update(id, data),
    remove: (id) => deps.repo.remove(id),
    listCached: () => deps.cache.importRules(),
    findById: (id) => deps.cache.findById('importRules', id),
    onChange: (cb) => deps.cache.subscribe('IMPORT_RULE', cb),
  };
}

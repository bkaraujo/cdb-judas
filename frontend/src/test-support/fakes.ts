/** Dublês compartilhados pelas suítes de service. Não é suíte: `vitest.config.ts` coleta só
 * `src/**\/*.test.ts`, e `.dependency-cruiser.cjs` exclui `src/test-support/` do grafo. */
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';

/** CacheStore mínimo. Passe só as coleções que a suíte usa. */
export function fakeCache(overrides: Partial<CacheStore> = {}): CacheStore {
  return {
    categories: () => [],
    accounts: () => [],
    tags: () => [],
    costCenters: () => [],
    importRules: () => [],
    findById: () => null,
    subscribe: () => () => {},
    hydrate: () => {},
    setImportRules: () => {},
    upsert: () => false,
    remove: () => false,
    ...overrides,
  } as unknown as CacheStore;
}

/** Repositório CRUD genérico: `create` devolve o payload com id fixo '99', `update` com o id
 * recebido, `remove` devolve null. É a forma que 5 das 6 cópias já tinham à mão. */
export function fakeCrudRepo<T>(list: T[] = [], overrides: Record<string, unknown> = {}) {
  return {
    list: () => Promise.resolve(list),
    create: (data: Record<string, unknown>) => Promise.resolve({ id: '99', ...data }),
    update: (id: string, data: Record<string, unknown>) => Promise.resolve({ id, ...data }),
    remove: () => Promise.resolve(null),
    ...overrides,
  };
}

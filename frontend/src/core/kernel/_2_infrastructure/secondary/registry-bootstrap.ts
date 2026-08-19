import type {
  AccountResponse,
  CategoryResponse,
  CostCenter,
  ImportRule,
  Tag,
} from '@/api/types.ts';

export interface RegistrySnapshot {
  categories: CategoryResponse[];
  accounts: AccountResponse[];
  tags: Tag[];
  costCenters: CostCenter[];
  importRules: ImportRule[];
}

export interface RegistryRepos {
  categories: { list(): Promise<CategoryResponse[] | null> };
  accounts: { list(): Promise<AccountResponse[] | null> };
  tags: { list(): Promise<Tag[] | null> };
  costCenters: { list(): Promise<CostCenter[] | null> };
  importRules: { list(): Promise<ImportRule[] | null> };
}

/** Initial hydration of the CBD cache (implementado por `cache-store`, Fase 4). */
export interface RegistryBootstrap {
  hydrate(): Promise<RegistrySnapshot>;
}

export function createRegistryBootstrap(repos: RegistryRepos): RegistryBootstrap {
  return {
    hydrate: async () => {
      const [categories, accounts, tags, costCenters, importRules] = await Promise.all([
        repos.categories.list(),
        repos.accounts.list(),
        repos.tags.list(),
        repos.costCenters.list(),
        repos.importRules.list(),
      ]);
      return {
        categories: Array.isArray(categories) ? categories : [],
        accounts: Array.isArray(accounts) ? accounts : [],
        tags: Array.isArray(tags) ? tags : [],
        costCenters: Array.isArray(costCenters) ? costCenters : [],
        importRules: Array.isArray(importRules) ? importRules : [],
      };
    },
  };
}

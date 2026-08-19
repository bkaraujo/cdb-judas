/** Category use cases. Sem domain própria — Domain.Category é kernel
 * (core/kernel/_0_domain/category.ts), usado pelos widgets genéricos de picker. */
import type { CategoryResponse, CreateRequest, UpdateRequest } from '@/api/types.ts';
import * as CategoryDomain from '@/core/kernel/_0_domain/category.ts';
import type { Category } from '@/core/kernel/_0_domain/category.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import type { CbdChangeDetail } from '@/core/kernel/_1_application/event-bus.ts';
import type { DeletionQueryOptions } from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';
import type { CategoryRepository } from '@/feature/categories/repository.ts';

export interface CategoryServiceDeps {
  repo: CategoryRepository;
  cache: CacheStore;
}

export interface CategoryService {
  list(): Promise<CategoryResponse[] | null>;
  create(data: CreateRequest): Promise<CategoryResponse | null>;
  update(id: string, data: UpdateRequest): Promise<CategoryResponse | null>;
  remove(id: string, opts?: DeletionQueryOptions): Promise<void | null>;
  listCached(): Category[];
  findById(id: string | null | undefined): Category | null;
  rootsByNature(nature: string): Category[];
  childrenOf(parentId: string | null | undefined): Category[];
  labelChain(id: string | null | undefined): string;
  eligibleParents(nature: string, excludeId: string | null | undefined): Category[];
  isEffectivelyActive(id: string | null | undefined): boolean;
  onChange(cb: (detail: CbdChangeDetail) => void): () => void;
}

export function createCategoryService(deps: CategoryServiceDeps): CategoryService {
  return {
    list: () => deps.repo.list(),
    create: (data) => deps.repo.create(data),
    update: (id, data) => deps.repo.update(id, data),
    remove: (id, opts) => deps.repo.remove(id, opts),
    listCached: () => deps.cache.categories(),
    findById: (id) => deps.cache.findById('categories', id),
    rootsByNature: (nature) => CategoryDomain.rootsByNature(deps.cache.categories(), nature),
    childrenOf: (parentId) => CategoryDomain.childrenOf(deps.cache.categories(), parentId),
    labelChain: (id) => CategoryDomain.labelChain(deps.cache.categories(), id),
    eligibleParents: (nature, excludeId) => CategoryDomain.eligibleParents(deps.cache.categories(), nature, excludeId),
    isEffectivelyActive: (id) => CategoryDomain.isEffectivelyActive(deps.cache.categories(), id),
    onChange: (cb) => deps.cache.subscribe('CATEGORY', cb),
  };
}

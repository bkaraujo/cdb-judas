/** Tag use cases. Sem domain própria — Domain.Tag é kernel (core/kernel/_0_domain/tag.ts), usado
 * pelos widgets genéricos de picker. */
import type { Tag as TagWire, TagRequest } from '@/api/types.ts';
import type { Tag } from '@/core/kernel/_0_domain/tag.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import type { CbdChangeDetail } from '@/core/kernel/_1_application/event-bus.ts';
import type { DeletionQueryOptions } from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';
import type { TagRepository } from '@/feature/tags/repository.ts';

export interface TagServiceDeps {
  repo: TagRepository;
  cache: CacheStore;
}

export interface TagService {
  list(): Promise<TagWire[] | null>;
  create(data: TagRequest): Promise<TagWire | null>;
  update(id: string, data: TagRequest): Promise<TagWire | null>;
  remove(id: string, opts?: DeletionQueryOptions): Promise<void | null>;
  listCached(): Tag[];
  findById(id: string | null | undefined): Tag | null;
  onChange(cb: (detail: CbdChangeDetail) => void): () => void;
}

export function createTagService(deps: TagServiceDeps): TagService {
  return {
    list: () => deps.repo.list(),
    create: (data) => deps.repo.create(data),
    update: (id, data) => deps.repo.update(id, data),
    remove: (id, opts) => deps.repo.remove(id, opts),
    listCached: () => deps.cache.tags(),
    findById: (id) => deps.cache.findById('tags', id),
    onChange: (cb) => deps.cache.subscribe('TAG', cb),
  };
}

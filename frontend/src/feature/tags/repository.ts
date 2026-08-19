/** HTTP adapter for /tags. */
import type { Tag as TagWire, TagRequest } from '@/api/types.ts';
import type { DeletionQueryOptions, HttpClient } from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';
import { deletionQuery } from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';

export interface TagRepository {
  list(): Promise<TagWire[] | null>;
  create(data: TagRequest): Promise<TagWire | null>;
  update(id: string, data: TagRequest): Promise<TagWire | null>;
  remove(id: string, opts?: DeletionQueryOptions): Promise<void | null>;
}

export function createTagRepository(http: HttpClient): TagRepository {
  return {
    list: () => http.get<TagWire[]>('/tags'),
    create: (data) => http.post<TagWire>('/tags', data),
    update: (id, data) => http.patch<TagWire>('/tags/' + id, data),
    remove: (id, opts) => http.delete('/tags/' + id + deletionQuery(opts)),
  };
}

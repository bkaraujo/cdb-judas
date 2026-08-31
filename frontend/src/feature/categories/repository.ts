/** HTTP adapter for /categories. */
import type {CategoryResponse, CreateRequest, UpdateRequest} from '@/api/types.ts';
import type {DeletionQueryOptions, HttpClient} from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';
import {deletionQuery} from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';

export interface CategoryRepository {
  list(): Promise<CategoryResponse[] | null>;
  create(data: CreateRequest): Promise<CategoryResponse | null>;
  update(id: string, data: UpdateRequest): Promise<CategoryResponse | null>;
  remove(id: string, opts?: DeletionQueryOptions): Promise<void | null>;
}

export function createCategoryRepository(http: HttpClient): CategoryRepository {
  return {
    list: () => http.get<CategoryResponse[]>('/categories'),
    create: (data) => http.post<CategoryResponse>('/categories', data),
    update: (id, data) => http.patch<CategoryResponse>('/categories/' + id, data),
    remove: (id, opts) => http.delete('/categories/' + id + deletionQuery(opts)),
  };
}

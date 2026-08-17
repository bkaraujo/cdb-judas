import type { SelfResponse, UpdateMeRequest } from '../../../../api/types.ts';
import type { HttpClient } from './http-client.ts';

/** HTTP adapter for the self resource (/api/me). Uses the global (no user-prefix) namespace:
 * identity comes from the authenticated context. */
export interface SelfRepository {
  get(): Promise<SelfResponse | null>;
  patch(data: UpdateMeRequest): Promise<SelfResponse | null>;
}

export function createSelfRepository(http: HttpClient): SelfRepository {
  return {
    get: () => http.global.get<SelfResponse>('/me'),
    patch: (data) => http.global.patch<SelfResponse>('/me', data),
  };
}

/** Global system resources. */
import type {HttpClient} from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';

export interface VersionResponse {
  version?: string;
}

export interface SystemService {
  getVersion(): Promise<VersionResponse | null>;
}

export function createSystemService(api: HttpClient): SystemService {
  return {
    getVersion: () => api.global.get<VersionResponse>('/version'),
  };
}

/** Typed access to the self resource (/api/me). */
import type {PreferencesRequest, SelfResponse} from '@/api/types.ts';
import type {SelfRepository} from '@/core/kernel/_2_infrastructure/secondary/self-repository.ts';

export interface SelfService {
  getMe(): Promise<SelfResponse | null>;
  updateName(name: string | null | undefined): Promise<SelfResponse | null>;
  updatePreferences(patch: Partial<PreferencesRequest>): Promise<SelfResponse | null>;
}

export function createSelfService(repo: SelfRepository): SelfService {
  return {
    getMe: () => repo.get(),
    // Partial PATCH /api/me — sends only the display name; preferences stay untouched.
    updateName: (name) => repo.patch({ name: name == null ? '' : name }),
    // Partial PATCH /api/me — sends only a preferences patch (server merges; nulls keep current).
    updatePreferences: (patch) => repo.patch({ preferences: patch }),
  };
}

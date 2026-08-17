import type { Storage } from './storage.ts';

const USER_KEY = 'auth_user';
const UID_KEY = 'auth_uid';
const NAME_KEY = 'auth_name';

export interface AuthStore {
  get(): string | null;
  set(token: string): void;
  clear(): void;
  setUser(user: string): void;
  decodeUser(): string;
  setUserId(id: string): void;
  userId(): string | null;
  /** Vazio/null limpa o nome, e o avatar cai para o username. */
  setName(name: string | null | undefined): void;
  name(): string | null;
}

export function createAuthStore(storage: Storage): AuthStore {
  const ss = storage.session;
  const AUTH_TOKEN = storage.KEYS.AUTH_TOKEN;
  return {
    get: () => ss.get(AUTH_TOKEN),
    set: (token) => ss.set(AUTH_TOKEN, token),
    clear: () => {
      ss.del(AUTH_TOKEN);
      ss.del(USER_KEY);
      ss.del(UID_KEY);
      ss.del(NAME_KEY);
    },
    setUser: (user) => ss.set(USER_KEY, user),
    decodeUser: () => ss.get(USER_KEY) || 'anonymous',
    setUserId: (id) => ss.set(UID_KEY, id),
    userId: () => ss.get(UID_KEY),
    setName: (name) => {
      if (name) ss.set(NAME_KEY, name);
      else ss.del(NAME_KEY);
    },
    name: () => ss.get(NAME_KEY),
  };
}

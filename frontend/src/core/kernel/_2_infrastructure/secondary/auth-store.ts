import type {Storage} from '@/core/kernel/_2_infrastructure/secondary/storage.ts';

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
  // Token e identidade em localStorage (compartilhado entre abas do mesmo origin).
  // Isso permite múltiplas abas simultâneas sem novo login: o token rotativo é sempre
  // lido via auth.get() no momento do disparo de cada requisição, então uma aba que
  // rotaciona o token já escreve em localStorage e as demais leem o valor atualizado.
  const ls = storage.local;
  const AUTH_TOKEN = storage.KEYS.AUTH_TOKEN;
  return {
    get: () => ls.get(AUTH_TOKEN),
    set: (token) => ls.set(AUTH_TOKEN, token),
    clear: () => {
      ls.del(AUTH_TOKEN);
      ls.del(USER_KEY);
      ls.del(UID_KEY);
      ls.del(NAME_KEY);
    },
    setUser: (user) => ls.set(USER_KEY, user),
    decodeUser: () => ls.get(USER_KEY) || 'anonymous',
    setUserId: (id) => ls.set(UID_KEY, id),
    userId: () => ls.get(UID_KEY),
    setName: (name) => {
      if (name) ls.set(NAME_KEY, name);
      else ls.del(NAME_KEY);
    },
    name: () => ls.get(NAME_KEY),
  };
}

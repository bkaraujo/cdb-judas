export interface StorageArea {
  get(key: string): string | null;
  set(key: string, value: string): void;
  del(key: string): void;
}

export interface LocalStorageArea extends StorageArea {
  json<T>(key: string): T | null;
  setJson(key: string, value: unknown): void;
}

export interface Storage {
  KEYS: typeof STORAGE_KEYS;
  local: LocalStorageArea;
  session: StorageArea;
}

/** Exposto pendurado no próprio objeto `Storage` (não como export solto): quem precisa de uma
 * chave recebe o `Storage` inteiro por DI e lê `storage.KEYS.X` — igual ao `deps.storage.KEYS` do
 * `preferences-service.js`/`auth-store.js` originais. Evita _1_application importar um valor
 * estático de _2_infrastructure/secondary só para pegar nomes de chave. */
export const STORAGE_KEYS = {
  THEME: 'cdb-theme',
  SCREEN: 'cdb-screen',
  SIDEBAR_COLLAPSED: 'cdb-sidebar-collapsed',
  SIDEBAR_GROUPS: 'cdb-sidebar-groups',
  DASHBOARD_SETTINGS: 'cdb-dashboard-settings',
  AUTH_TOKEN: 'auth_token',
} as const;

/** Um único wrapper try/catch por área — `localStorage` e `sessionStorage` diferem só no
 * objeto nativo. Acesso pode lançar (modo privado, cota, iframe sem permissão), por isso
 * toda operação é engolida: leitura falha vira `null`, escrita falha vira no-op. */
function area(store: globalThis.Storage): StorageArea {
  return {
    get(key) {
      try {
        return store.getItem(key);
      } catch {
        return null;
      }
    },
    set(key, value) {
      try {
        store.setItem(key, value);
      } catch {
        /* noop */
      }
    },
    del(key) {
      try {
        store.removeItem(key);
      } catch {
        /* noop */
      }
    },
  };
}

function localArea(): LocalStorageArea {
  const base = area(localStorage);
  return {
    ...base,
    json<T>(key: string): T | null {
      const raw = base.get(key);
      if (!raw) return null;
      try {
        return JSON.parse(raw) as T;
      } catch {
        return null;
      }
    },
    setJson(key: string, value: unknown): void {
      base.set(key, JSON.stringify(value));
    },
  };
}

export function createStorage(): Storage {
  return {
    KEYS: STORAGE_KEYS,
    local: localArea(),
    session: area(sessionStorage),
  };
}

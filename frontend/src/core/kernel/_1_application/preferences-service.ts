/**
 * Server-owned user preferences.
 *
 * localStorage acts as a MIRROR CACHE of the server preferences so the app can apply them
 * instantly at boot (no theme flash). Every change is WRITE-THROUGH: applied to the mirror
 * immediately and pushed to the server via a debounced PATCH /api/me. Works offline (kept
 * pending) and re-syncs on reconnect. At login, applyServer() reconciles: the server wins, except
 * when the server theme is null — then the client value is taught back to the server.
 *
 * Server-synced fields: theme, language, locale, sidebarCollapsed.
 * Local-only (not synced): last screen, sidebar groups, dashboard settings.
 */
import type { AuthStore } from '@/core/kernel/_2_infrastructure/secondary/auth-store.ts';
import type { Storage } from '@/core/kernel/_2_infrastructure/secondary/storage.ts';

const MIRROR_KEY = 'cdb-preferences';
const DEBOUNCE_MS = 500;

export interface LocalPreferences {
  theme: string | null;
  language: string;
  locale: string;
  sidebarCollapsed: boolean;
}

const DEFAULTS: LocalPreferences = { theme: null, language: 'pt-BR', locale: 'pt-BR', sidebarCollapsed: false };
const SYNCED: (keyof LocalPreferences)[] = ['theme', 'language', 'locale', 'sidebarCollapsed'];

export interface PreferencesServiceDeps {
  storage: Storage;
  authStore: AuthStore;
  sync?: (patch: Partial<LocalPreferences>) => Promise<unknown>;
}

export interface PreferencesService {
  get<K extends keyof LocalPreferences>(key: K): LocalPreferences[K];
  set<K extends keyof LocalPreferences>(key: K, value: LocalPreferences[K]): void;
  flush(): Promise<unknown>;
  applyServer(serverPrefs: Partial<LocalPreferences> | null | undefined): void;
  getTheme(): string | null;
  setTheme(t: string): void;
  getLastScreen(): string | null;
  getSidebarCollapsed(): boolean;
  setSidebarCollapsed(b: boolean): void;
  getSidebarGroups(): Record<string, unknown>;
  setSidebarGroups(obj: Record<string, unknown>): void;
  getDashboardSettings(): unknown;
  saveDashboardSettings(s: unknown): void;
}

export function createPreferencesService(deps: PreferencesServiceDeps): PreferencesService {
  const storage = deps.storage;
  const sync = deps.sync || (() => Promise.resolve());

  function readMirror(): LocalPreferences {
    const raw = storage.local.json<Partial<LocalPreferences>>(MIRROR_KEY);
    return raw && typeof raw === 'object' ? { ...DEFAULTS, ...raw } : { ...DEFAULTS };
  }
  function writeMirror(): void {
    storage.local.setJson(MIRROR_KEY, prefs);
  }

  let prefs: LocalPreferences = readMirror();
  let pending: Partial<LocalPreferences> = {};
  let timer: ReturnType<typeof setTimeout> | null = null;

  // One-time pickup of values from the pre-mirror single-key layout.
  function migrateLegacy(): void {
    if (prefs.theme == null) {
      const t = storage.local.get(storage.KEYS.THEME);
      if (t) prefs.theme = t;
    }
    const sc = storage.local.get(storage.KEYS.SIDEBAR_COLLAPSED);
    if (sc != null) prefs.sidebarCollapsed = sc === '1';
    writeMirror();
  }
  migrateLegacy();
  if (typeof window !== 'undefined' && window.addEventListener) {
    window.addEventListener('online', () => {
      flush();
    });
  }

  function isAuthenticated(): boolean {
    return !!deps.authStore.get();
  }

  function get<K extends keyof LocalPreferences>(key: K): LocalPreferences[K] {
    return prefs[key];
  }

  function set<K extends keyof LocalPreferences>(key: K, value: LocalPreferences[K]): void {
    if (prefs[key] === value) return;
    prefs[key] = value;
    writeMirror();
    if (SYNCED.indexOf(key) >= 0) {
      pending[key] = value;
      schedule();
    }
  }

  function schedule(): void {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      flush();
    }, DEBOUNCE_MS);
  }

  function flush(): Promise<unknown> {
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }
    const keys = Object.keys(pending) as (keyof LocalPreferences)[];
    if (!keys.length) return Promise.resolve();
    if (!isAuthenticated()) return Promise.resolve(); // defer; login reconciliation decides

    const patch: Partial<LocalPreferences> = {};
    keys.forEach((k) => {
      (patch as Record<string, unknown>)[k] = pending[k];
    });
    return sync(patch)
      .then(() => {
        // Clear only fields still holding the value we sent (a newer change re-queues them).
        keys.forEach((k) => {
          if (pending[k] === patch[k]) delete pending[k];
        });
      })
      .catch(() => {
        /* offline/failed: keep pending; retry on 'online' or next change */
      });
  }

  // Reconciliation at login: server wins; server theme null → adopt + teach the client value.
  function applyServer(serverPrefs: Partial<LocalPreferences> | null | undefined): void {
    if (!serverPrefs) return;
    const local = prefs || readMirror();
    pending = {}; // drop unsynced local changes — the server is the source of truth
    const merged: LocalPreferences = { ...DEFAULTS, ...serverPrefs };
    if (serverPrefs.theme == null && local.theme != null) {
      merged.theme = local.theme;
      pending.theme = local.theme;
    }
    prefs = merged;
    writeMirror();
    if (Object.keys(pending).length) schedule();
  }

  return {
    get,
    set,
    flush,
    applyServer,
    getTheme: () => prefs.theme,
    setTheme: (t) => set('theme', t),
    getSidebarCollapsed: () => !!prefs.sidebarCollapsed,
    setSidebarCollapsed: (b) => set('sidebarCollapsed', !!b),
    getLastScreen: () => storage.local.get(storage.KEYS.SCREEN),
    getSidebarGroups: () => storage.local.json<Record<string, unknown>>(storage.KEYS.SIDEBAR_GROUPS) || {},
    setSidebarGroups: (obj) => storage.local.setJson(storage.KEYS.SIDEBAR_GROUPS, obj),
    getDashboardSettings: () => storage.local.json(storage.KEYS.DASHBOARD_SETTINGS),
    saveDashboardSettings: (s) => storage.local.setJson(storage.KEYS.DASHBOARD_SETTINGS, s),
  };
}

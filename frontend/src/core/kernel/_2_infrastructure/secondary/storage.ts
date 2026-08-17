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
  local: LocalStorageArea;
  session: StorageArea;
}

export const STORAGE_KEYS = {
  THEME: 'cbd-theme',
  SCREEN: 'cbd-screen',
  SIDEBAR_COLLAPSED: 'cbd-sidebar-collapsed',
  SIDEBAR_GROUPS: 'cbd-sidebar-groups',
  DASHBOARD_SETTINGS: 'cbd-dashboard-settings',
  AUTH_TOKEN: 'auth_token',
} as const;

function lsGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}
function lsSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* noop */
  }
}
function lsDel(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    /* noop */
  }
}
function lsJson<T>(key: string): T | null {
  const raw = lsGet(key);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}
function lsSetJson(key: string, value: unknown): void {
  lsSet(key, JSON.stringify(value));
}

function ssGet(key: string): string | null {
  try {
    return sessionStorage.getItem(key);
  } catch {
    return null;
  }
}
function ssSet(key: string, value: string): void {
  try {
    sessionStorage.setItem(key, value);
  } catch {
    /* noop */
  }
}
function ssDel(key: string): void {
  try {
    sessionStorage.removeItem(key);
  } catch {
    /* noop */
  }
}

export function createStorage(): Storage {
  return {
    local: { get: lsGet, set: lsSet, del: lsDel, json: lsJson, setJson: lsSetJson },
    session: { get: ssGet, set: ssSet, del: ssDel },
  };
}

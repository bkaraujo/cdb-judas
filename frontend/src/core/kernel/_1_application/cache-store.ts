/**
 * Read-only access to CBD cache + subscribe.
 *
 * O cache (`window.CBD` no original) vira estado privado do módulo, escrito só por `hydrate()`
 * (carga inicial, chamada pelo composition-root com o snapshot de `registry-bootstrap`, Fase 2) e
 * por `upsert`/`remove` (atualizações via SSE — implementa a porta `SseCachePort` que
 * `sse-client.ts`, Fase 2, consome). Páginas leem por aqui e reagem a mudanças via
 * `subscribe(type, cb)`.
 */
import type { AccountResponse, CategoryResponse, CostCenter, ImportRule, Tag as TagWire } from '../../../api/types.ts';
import * as Account from '../_0_domain/account.ts';
import type { Account as AccountEntity } from '../_0_domain/account.ts';
import * as Category from '../_0_domain/category.ts';
import type { Category as CategoryEntity } from '../_0_domain/category.ts';
import * as Tag from '../_0_domain/tag.ts';
import type { Tag as TagEntity } from '../_0_domain/tag.ts';
import type { RegistrySnapshot } from '../_2_infrastructure/secondary/registry-bootstrap.ts';
import type { CbdChangeDetail, EventBus } from './event-bus.ts';

export type CbdEntityKind = 'categories' | 'accounts' | 'tags';

export interface CbdStore {
  categories: CategoryEntity[];
  accounts: AccountEntity[];
  tags: TagEntity[];
  costCenters: CostCenter[];
  importRules: ImportRule[];
}

/** Porta consumida por sse-client (Fase 2): aplica upsert/delete às três coleções com canal de
 * eventos — centros de custo e regras de importação são fixos, sem canal SSE. */
export interface SseCachePort {
  upsert(kind: CbdEntityKind, payload: Record<string, unknown>): boolean;
  remove(kind: CbdEntityKind, id: string): boolean;
}

export type CacheSubscribeFilter = string | string[] | null;

export interface CacheStore extends SseCachePort {
  categories(): CategoryEntity[];
  accounts(): AccountEntity[];
  tags(): TagEntity[];
  costCenters(): CostCenter[];
  importRules(): ImportRule[];
  findById<K extends keyof CbdStore>(key: K, id: string | null | undefined): CbdStore[K][number] | null;
  subscribe(typeFilter: CacheSubscribeFilter, cb: (detail: CbdChangeDetail) => void): () => void;
  /** Popula o cache com o snapshot inicial. Normaliza categorias/contas/tags pelas mesmas regras
   * de domínio usadas nas atualizações via SSE — `window.CBD` original guardava o payload cru na
   * carga inicial e só normalizava (parcialmente, só categoria/conta) nas atualizações via SSE;
   * aqui as duas fontes convergem para o mesmo formato. */
  hydrate(snapshot: RegistrySnapshot): void;
}

export function createCacheStore(bus: EventBus): CacheStore {
  let store: CbdStore = { categories: [], accounts: [], tags: [], costCenters: [], importRules: [] };

  function findById<K extends keyof CbdStore>(key: K, id: string | null | undefined): CbdStore[K][number] | null {
    if (id == null) return null;
    const arr = store[key];
    for (let i = 0; i < arr.length; i++) {
      const item = arr[i];
      if (item && String(item.id) === String(id)) return item;
    }
    return null;
  }

  /* Subscribe to cbd:change. `typeFilter` may be a string, an array of strings, or null (no
   * filter). */
  function subscribe(typeFilter: CacheSubscribeFilter, cb: (detail: CbdChangeDetail) => void): () => void {
    const accepts = Array.isArray(typeFilter)
      ? (t: string) => typeFilter.indexOf(t) >= 0
      : typeof typeFilter === 'string'
        ? (t: string) => t === typeFilter
        : () => true;
    const handler = (_evt: unknown, detail: CbdChangeDetail) => {
      if (!detail || !accepts(detail.type)) return;
      try {
        cb(detail);
      } catch {
        /* noop */
      }
    };
    bus.on('cbd:change', handler);
    return () => bus.off('cbd:change', handler);
  }

  function upsert(kind: CbdEntityKind, payload: Record<string, unknown>): boolean {
    if (kind === 'categories') {
      const normalized = Category.normalize(payload as CategoryResponse);
      if (!normalized) return false;
      const list = store.categories.slice();
      const idx = list.findIndex((x) => String(x.id) === String(normalized.id));
      if (idx >= 0) list[idx] = normalized;
      else list.push(normalized);
      store = { ...store, categories: list };
      return true;
    }
    if (kind === 'accounts') {
      const normalized = Account.normalize(payload as AccountResponse);
      if (!normalized) return false;
      const list = store.accounts.slice();
      const idx = list.findIndex((x) => String(x.id) === String(normalized.id));
      if (idx >= 0) list[idx] = normalized;
      else list.push(normalized);
      store = { ...store, accounts: list };
      return true;
    }
    const normalized = Tag.normalize(payload as TagWire);
    if (!normalized) return false;
    const list = store.tags.slice();
    const idx = list.findIndex((x) => String(x.id) === String(normalized.id));
    if (idx >= 0) list[idx] = normalized;
    else list.push(normalized);
    store = { ...store, tags: list };
    return true;
  }

  function remove(kind: CbdEntityKind, id: string): boolean {
    if (id == null) return false;
    if (kind === 'categories') {
      store = { ...store, categories: store.categories.filter((x) => String(x.id) !== String(id)) };
      return true;
    }
    if (kind === 'accounts') {
      store = { ...store, accounts: store.accounts.filter((x) => String(x.id) !== String(id)) };
      return true;
    }
    store = { ...store, tags: store.tags.filter((x) => String(x.id) !== String(id)) };
    return true;
  }

  return {
    categories: () => store.categories,
    accounts: () => store.accounts,
    tags: () => store.tags,
    costCenters: () => store.costCenters,
    importRules: () => store.importRules,
    findById,
    subscribe,
    upsert,
    remove,
    hydrate(snapshot) {
      store = {
        categories: snapshot.categories.map(Category.normalize).filter((c): c is CategoryEntity => c != null),
        accounts: snapshot.accounts.map(Account.normalize).filter((a): a is AccountEntity => a != null),
        tags: snapshot.tags.map(Tag.normalize).filter((t): t is TagEntity => t != null),
        costCenters: snapshot.costCenters,
        importRules: snapshot.importRules,
      };
    },
  };
}

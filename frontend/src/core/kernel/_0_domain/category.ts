import type { CategoryResponse } from '../../../api/types.ts';

export const NATURE = { EXPENSE: 'EXPENSE', INCOME: 'INCOME' } as const;
export type Nature = (typeof NATURE)[keyof typeof NATURE];

export interface Category {
  id: string;
  name: string;
  nature: string;
  parentId: string | null;
  isSystem: boolean;
  active: boolean;
}

/** `raw.description` (fallback de `name`) e `raw.parent.id` (fallback de `parentId`) não existem
 * em `CategoryResponse` — remoção pré-aprovada. */
export function normalize(raw: CategoryResponse | null | undefined): Category | null {
  if (!raw || raw.id == null) return null;
  return {
    id: raw.id,
    name: raw.name || '',
    nature: String(raw.nature || NATURE.EXPENSE).toUpperCase(),
    parentId: raw.parentId ?? null,
    isSystem: !!raw.isSystem,
    active: raw.active !== false,
  };
}

export function isExpense(c: Category | null | undefined): boolean {
  return !!c && c.nature === NATURE.EXPENSE;
}
export function isRevenue(c: Category | null | undefined): boolean {
  return !!c && c.nature === NATURE.INCOME;
}
export function isRoot(c: Category | null | undefined): boolean {
  return !!c && c.parentId == null;
}

export function byId(list: readonly Category[] | null | undefined, id: string | null | undefined): Category | null {
  if (id == null) return null;
  const items = list || [];
  for (let i = 0; i < items.length; i++) {
    if (String(items[i]?.id) === String(id)) return items[i] as Category;
  }
  return null;
}

export function filterByNature(list: readonly Category[] | null | undefined, nature: string): Category[] {
  return (list || []).filter((c) => c.nature === nature);
}

export function rootsByNature(list: readonly Category[] | null | undefined, nature: string): Category[] {
  return filterByNature(list, nature).filter(isRoot);
}

export function childrenOf(list: readonly Category[] | null | undefined, parentId: string | null | undefined): Category[] {
  if (parentId == null) return [];
  return (list || []).filter((c) => c.parentId != null && String(c.parentId) === String(parentId));
}

/** Parent → child label, e.g. "Alimentação / Restaurante". */
export function labelChain(list: readonly Category[] | null | undefined, id: string | null | undefined, separator?: string): string {
  const sep = separator || ' · ';
  const c = byId(list, id);
  if (!c) return '';
  if (c.parentId == null) return c.name;
  const parent = byId(list, c.parentId);
  return (parent ? parent.name + sep : '') + c.name;
}

/** For a parent picker on create/edit: roots of same nature, excluding self. */
export function eligibleParents(list: readonly Category[] | null | undefined, nature: string, excludeId: string | null | undefined): Category[] {
  return rootsByNature(list, nature).filter((c) => String(c.id) !== String(excludeId));
}

/** A category is only usable for new classification when it AND every ancestor are active —
 * inactivating a macro hides its subcategories too, without touching their own flag. */
export function isEffectivelyActive(list: readonly Category[] | null | undefined, id: string | null | undefined): boolean {
  let c = byId(list, id);
  while (c) {
    if (c.active === false) return false;
    c = c.parentId != null ? byId(list, c.parentId) : null;
  }
  return true;
}

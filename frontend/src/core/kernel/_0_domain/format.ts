/**
 * Value/date formatters + html escape. Pure — sem DOM (classificação que elimina a exceção de
 * camada `money → format` que `web/tools/check-slices.js:32-35` precisava manter).
 *
 * `bindCurrencyMask` do `format.js` original não é portado aqui: liga um handler jQuery a um
 * `<input>` (efeito colateral de DOM), não é formatação pura — vai para a camada `primary` na
 * Fase 5. As demais funções abaixo que liam `window.App.CacheStore`/`window.Domain.*` viram
 * puras: recebem a coleção como parâmetro em vez de ler estado global (transformação 1 do plano).
 */
import type { Account } from './account.ts';
import * as Category from './category.ts';
import type { Category as CategoryEntity } from './category.ts';
import * as Period from './period.ts';

export function pad2(n: number): string {
  return n < 10 ? '0' + n : '' + n;
}

// Expects month to be 1-based (1-12)
export function monthLabel(month: number, year: number): string {
  const d = new Date(year, month - 1, 1);
  const s = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(d);
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function sortByName(a: { name?: string | null }, b: { name?: string | null }): number {
  return String(a.name || '')
    .toLowerCase()
    .localeCompare(String(b.name || '').toLowerCase(), 'pt-BR');
}

/** Always returns ABSOLUTE value formatted as BR currency (no minus sign). Use valueColor(v) /
 * fmtSigned(v) to convey sign via color. */
export function fmt(v: number | string | null | undefined): string {
  const n = Math.abs(+(v ?? 0) || 0);
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(n);
}

export function fmtShort(v: number | string | null | undefined): string {
  const abs = Math.abs(+(v ?? 0) || 0);
  if (abs >= 1000) {
    return 'R$ ' + (abs / 1000).toFixed(1).replace('.', ',') + 'k';
  }
  return fmt(abs);
}

/** Returns CSS color token for a numeric value: green if >=0, red if <0. */
export function valueColor(v: number | string | null | undefined): string {
  return (+(v ?? 0) || 0) < 0 ? 'var(--expense)' : 'var(--income)';
}

/** Returns ready-to-inject HTML span: absolute value, colored by sign. */
export function fmtSigned(v: number | string | null | undefined): string {
  return '<span style="color:' + valueColor(v) + ';">' + fmt(v) + '</span>';
}

/** Parse a date value to a Date in LOCAL time. A bare "YYYY-MM-DD" is parsed by Date() as UTC
 * midnight, which renders as the previous day in negative-offset zones (e.g. BRT). Build the
 * date from its parts to keep it on the local day. */
export function parseLocalDate(d: string | Date | null | undefined): Date {
  if (d instanceof Date) return d;
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(d || ''));
  return m ? new Date(+m[1]!, +m[2]! - 1, +m[3]!) : new Date(d as string);
}

export function fmtDate(d: string | Date | null | undefined): string {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(parseLocalDate(d));
}

export function fmtMonth(d: string | Date | null | undefined): string {
  return new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(parseLocalDate(d)).replace('.', '');
}

/** Parse "1.234,56" / "R$ 1.234,56" / "1234.56" / number → Number (NaN if invalid). */
export function parseCurrency(v: number | string | null | undefined): number {
  if (typeof v === 'number') return v;
  if (v == null) return NaN;
  let s = String(v).replace(/[^\d,.-]/g, '');
  if (s.indexOf(',') >= 0) s = s.replace(/\./g, '').replace(',', '.');
  return parseFloat(s);
}

/** Mask number / string as "1.234,56" (no R$). Used for currency inputs. */
export function maskCurrency(v: number | string | null | undefined): string {
  let n = parseCurrency(v);
  if (!isFinite(n)) n = 0;
  return n.toFixed(2).replace('.', ',').replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/** HTML-escape helper. Always wrap interpolated values from API in esc(). */
export function esc(s: unknown): string {
  if (s === null || s === undefined) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function categoryById(categories: readonly CategoryEntity[]): Record<string, CategoryEntity> {
  const map: Record<string, CategoryEntity> = {};
  categories.forEach((c) => {
    map[c.id] = c;
  });
  return map;
}

export function categoryLabel(categories: readonly CategoryEntity[], c: { id: string } | null | undefined): string {
  if (!c) return '';
  return Category.labelChain(categories, c.id, ' / ');
}

export interface FlatCategory {
  id: string;
  label: string;
  nature: string;
}

/** keepId: include this category even if it (or an ancestor) is inactive — keeps a transaction
 * being edited from silently losing its category when the dropdown is rebuilt. Only applies when
 * natureFilter is set; the no-filter form (width-measurement callers) is left untouched. */
export function flatCategories(
  categories: readonly CategoryEntity[],
  natureFilter?: string | null,
  excludeRoots?: boolean,
  keepId?: string | null,
): FlatCategory[] {
  return categories
    .filter((c) => {
      if (excludeRoots && Category.isRoot(c)) return false;
      if (!natureFilter) return true;
      if (String(c.nature || '').toUpperCase() !== natureFilter) return false;
      return Category.isEffectivelyActive(categories, c.id) || String(c.id) === String(keepId);
    })
    .map((c) => ({ id: c.id, label: categoryLabel(categories, c), nature: c.nature }))
    .sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase(), 'pt-BR'));
}

export function accountsList(accounts: readonly Account[]): Account[] {
  return accounts.slice().sort(sortByName);
}

export function monthBounds(month: number, year: number): Period.PeriodBounds {
  return Period.bounds(Period.create(month + 1, year));
}

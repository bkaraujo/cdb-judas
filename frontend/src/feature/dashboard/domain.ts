/** Regras puras de agregação do Dashboard: balanço patrimonial + análises (despesa por
 * categoria, série mensal, próximos vencimentos). Sem domain própria de payable/budget/cartão —
 * a fatia lê essas via AccountsPayableApi/BudgetApi/CreditCardsApi (fatias irmãs, já fechadas). */
import type {Account} from '@/core/kernel/_0_domain/account.ts';
import * as AccountDomain from '@/core/kernel/_0_domain/account.ts';
import type {Category} from '@/core/kernel/_0_domain/category.ts';
import * as CategoryDomain from '@/core/kernel/_0_domain/category.ts';

export interface BalanceSheet {
  assets: number;
  liabilities: number;
  equity: number;
}

// No account type is a liability post-card-remodel (cards are no longer accounts) — everything
// sits on the ATIVO side until the model grows a real liability account type.
export function compute(accounts: readonly Account[] | null | undefined): BalanceSheet {
  let assets = 0;
  (accounts || []).forEach((a) => {
    assets += AccountDomain.currentBalance(a);
  });
  return { assets, liabilities: 0, equity: assets };
}

export interface AggTx {
  type?: string | null;
  amount?: number | string | null;
  categoryId?: string | null;
  date?: string | null;
}

export function txIsExpense(t: AggTx | null | undefined, categoryNature?: string | null): boolean {
  if (!t) return false;
  if (t.type) {
    const s = String(t.type).toLowerCase();
    if (s === 'expense') return true;
    if (s === 'income' || s === 'revenue') return false;
  }
  if (categoryNature) return String(categoryNature).toUpperCase() === 'EXPENSE';
  return (Number(t.amount) || 0) < 0;
}

export function categoryNameFor(t: { categoryId?: string | null } | null | undefined, categories: readonly Category[]): string {
  if (!t || t.categoryId == null) return 'Sem categoria';
  const c = CategoryDomain.byId(categories, t.categoryId);
  return c ? c.name : 'Sem categoria';
}

export function categoryNatureFor(t: { categoryId?: string | null } | null | undefined, categories: readonly Category[]): string | null {
  if (!t || t.categoryId == null) return null;
  const c = CategoryDomain.byId(categories, t.categoryId);
  return c ? c.nature : null;
}

export interface CategoryTotal {
  name: string;
  value: number;
}

/** Sum |amount| per category name across expense transactions. */
export function expenseByCategory(transactions: readonly AggTx[] | null | undefined, categories: readonly Category[]): CategoryTotal[] {
  const out: Record<string, number> = {};
  (transactions || []).forEach((t) => {
    const nature = categoryNatureFor(t, categories);
    if (!txIsExpense(t, nature)) return;
    const name = categoryNameFor(t, categories);
    out[name] = (out[name] || 0) + Math.abs(Number(t.amount) || 0);
  });
  return Object.keys(out)
    .map((k) => ({ name: k, value: out[k] || 0 }))
    .sort((a, b) => b.value - a.value);
}

export function topN<T>(items: readonly T[] | null | undefined, n?: number): T[] {
  return (items || []).slice(0, n || 5);
}

export interface MonthlyBucket {
  month: number;
  year: number;
  receitas: number;
  despesas: number;
}

/** Monthly receitas/despesas series over the last `monthsBack` months. `now` injetado —
 * determinístico, sem `Date.now()` implícito. */
export function monthlySeries(transactions: readonly AggTx[] | null | undefined, monthsBack?: number, now?: Date): MonthlyBucket[] {
  const m = monthsBack || 4;
  const ref = now || new Date();
  const buckets: MonthlyBucket[] = [];
  for (let i = m - 1; i >= 0; i--) {
    const d = new Date(ref.getFullYear(), ref.getMonth() - i, 1);
    buckets.push({ month: d.getMonth() + 1, year: d.getFullYear(), receitas: 0, despesas: 0 });
  }
  (transactions || []).forEach((t) => {
    const d = new Date(t.date || '');
    const idx = buckets.findIndex((b) => b.month === d.getMonth() + 1 && b.year === d.getFullYear());
    if (idx < 0) return;
    const bucket = buckets[idx];
    if (!bucket) return;
    const v = Math.abs(Number(t.amount) || 0);
    if (txIsExpense(t)) bucket.despesas += v;
    else bucket.receitas += v;
  });
  return buckets;
}

/** Filter & sort upcoming active payables by due/date ascending. `isActive` é injetado
 * (AccountsPayableApi.isActive, fatia irmã) — domain fica pura, sem importar accounts-payable. */
export function upcomingPayables<T extends { due?: string | null; date?: string | null }>(payables: readonly T[] | null | undefined, isActive: (item: T) => boolean): T[] {
  return (payables || [])
    .filter(isActive)
    .slice()
    .sort((a, b) => {
      const da = new Date(a.due || a.date || 0).getTime();
      const db = new Date(b.due || b.date || 0).getTime();
      return da - db;
    });
}

/** Regras puras de item de orçamento (fatia Metas/Orçamento). */
import {thresholdColorToken} from '@/core/kernel/_2_infrastructure/primary/ui/progress-bar.ts';
import type {BudgetResponse} from '@/feature/budget/types.ts';

export interface Budget {
  id: string;
  categoryId: string;
  name: string;
  budgeted: number;
  spent: number;
  month: number;
  year: number;
  color: string | null;
  icon: string | null;
}

export function normalize(raw: BudgetResponse | null | undefined): Budget | null {
  if (!raw) return null;
  return {
    id: raw.id,
    categoryId: raw.categoryId,
    name: raw.name || '',
    budgeted: +raw.budgeted || 0,
    spent: +raw.spent || 0,
    month: raw.month,
    year: raw.year,
    color: raw.color || null,
    icon: raw.icon || null,
  };
}

export function consumptionPct(spent: number, budgeted: number): number {
  const b = +budgeted || 0;
  if (b <= 0) return 0;
  return Math.min(100, Math.max(0, (Math.abs(+spent || 0) / b) * 100));
}

export function isOverBudget(spent: number, budgeted: number): boolean {
  const b = +budgeted || 0;
  return b > 0 && Math.abs(+spent || 0) > b;
}

export interface RemainingOrOver {
  value: number;
  over: boolean;
}

/** Retorna { value, over } — `value` é o restante (positivo) ou o valor estourado (positivo),
 * `over` indica qual dos dois. */
export function remainingOrOver(spent: number, budgeted: number): RemainingOrOver {
  const b = +budgeted || 0;
  const s = Math.abs(+spent || 0);
  if (s > b) return { value: s - b, over: true };
  return { value: b - s, over: false };
}

/** Espelha STYLE.md §11 (limiar compartilhado no kernel). */
export function barColor(pct: number): string {
  return thresholdColorToken(pct);
}

export function overspendCount(items: readonly { spent: number; budgeted: number }[] | null | undefined): number {
  return (items || []).reduce((acc, b) => acc + (isOverBudget(b.spent, b.budgeted) ? 1 : 0), 0);
}

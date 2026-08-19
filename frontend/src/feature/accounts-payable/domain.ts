/** Payable / Receivable rules. Pure. */
import { statusBadgeVariant as transactionStatusBadgeVariant } from '@/core/kernel/_0_domain/transaction.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';

export const PAYABLE_TYPES = { PAYABLE: 'PAYABLE', RECEIVABLE: 'RECEIVABLE' } as const;
export const PAYABLE_STATUS = { PENDING: 'pending', SCHEDULED: 'scheduled', CONFIRMED: 'confirmed', CANCELLED: 'cancelled' } as const;

export interface Payable {
  id: string;
  name: string;
  amount: number;
  due: string | null;
  status: string;
  categoryId?: string | null;
  accountId?: string | null;
  type: string;
}

interface PayableWireLike {
  id?: string;
  name?: string;
  description?: string;
  amount?: number | string;
  due?: string | null;
  date?: string | null;
  status?: string;
  categoryId?: string | null;
  accountId?: string | null;
  type?: string;
}

export function normalize(raw: PayableWireLike | null | undefined): Payable | null {
  if (!raw) return null;
  return {
    id: raw.id as string,
    name: raw.name || raw.description || '',
    amount: +(raw.amount ?? 0) || 0,
    due: raw.due || raw.date || null,
    status: String(raw.status || PAYABLE_STATUS.PENDING).toLowerCase(),
    categoryId: raw.categoryId,
    accountId: raw.accountId,
    type: String(raw.type || PAYABLE_TYPES.PAYABLE).toUpperCase(),
  };
}

export function isReceivable(p: { type?: string } | null | undefined): boolean {
  return !!p && p.type === PAYABLE_TYPES.RECEIVABLE;
}
export function isPayable(p: { type?: string } | null | undefined): boolean {
  return !!p && p.type === PAYABLE_TYPES.PAYABLE;
}
export function isActive(p: { status?: string } | null | undefined): boolean {
  return !!p && p.status !== PAYABLE_STATUS.CANCELLED;
}

/** Transaction nature derived from payable type. */
export function natureOf(typeOrPayable: string | { type?: string } | null | undefined): string {
  const t = typeof typeOrPayable === 'string' ? typeOrPayable : typeOrPayable && typeOrPayable.type;
  return String(t || '').toUpperCase() === PAYABLE_TYPES.RECEIVABLE ? 'income' : 'expense';
}

export interface PayableItem {
  amount: number;
  due: string | null;
  status: string;
  type: string;
}

/** Filter list by due date inside a Period. */
export function inPeriod<T extends PayableItem>(items: readonly T[] | null | undefined, period: Period.Period): T[] {
  return (items || []).filter((p) => {
    if (!p.due) return false;
    return Period.containsDate(period, p.due);
  });
}

export interface PayableTotals {
  payable: number;
  receivable: number;
  result: number;
}

export function periodTotals<T extends PayableItem>(items: readonly T[] | null | undefined, period: Period.Period): PayableTotals {
  const inP = inPeriod(items, period);
  let payable = 0;
  let receivable = 0;
  for (let i = 0; i < inP.length; i++) {
    const p = inP[i] as T;
    if (!isActive(p)) continue;
    const v = Math.abs(p.amount);
    if (isReceivable(p)) receivable += v;
    else payable += v;
  }
  return { payable, receivable, result: receivable - payable };
}

export const statusBadgeVariant = transactionStatusBadgeVariant;

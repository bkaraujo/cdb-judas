/** Credit card rules (uses Account + Period). Pure. */
import type { Account } from '../../core/kernel/_0_domain/account.ts';
import * as Invoice from '../../core/kernel/_0_domain/invoice.ts';
import type { Period } from '../../core/kernel/_0_domain/period.ts';
import { thresholdColorToken } from '../../core/kernel/_2_infrastructure/primary/ui/progress-bar.ts';

export const DEFAULT_CLOSING_DAY = 1;
export const DEFAULT_DUE_DAY = 10;

/* Closing/due day are configured once per account (MON_ACCOUNT_LIMIT) and shared by every card
 * on it — so these read from the account, not the card. */
export function closingDay(account: Account | null | undefined): number {
  const d = account ? +(account.closingDay ?? NaN) : NaN;
  return d > 0 ? d : DEFAULT_CLOSING_DAY;
}

export function dueDay(account: Account | null | undefined): number {
  const d = account ? +(account.dueDay ?? NaN) : NaN;
  return d > 0 ? d : DEFAULT_DUE_DAY;
}

/** Janela de compras da fatura que VENCE em `period` — delega o ciclo a Domain.Invoice. Não é o
 * mês-calendário: com closingDay=20 e dueDay=5, a fatura que vence em 05/04 cobre 21/02–20/03.
 * Conta sem vencimento no período devolve uma janela vazia (from > to), o que zera os totais
 * abaixo. */
export function invoicePeriod(account: Account | null | undefined, period: Period): { from: string; to: string } {
  const dues = Invoice.dueDatesIn(account, period);
  if (!dues.length) return { from: '9999-12-31', to: '0000-01-01' };
  return Invoice.cycleFor(account, dues[0] as string);
}

export function usagePct(used: number, limit: number): number {
  const u = Math.abs(+used || 0);
  const l = +limit || 0;
  if (l <= 0) return 0;
  return Math.min(100, Math.max(0, (u / l) * 100));
}

export function availableCredit(limit: number, used: number): number {
  return Math.max(0, (+limit || 0) - Math.abs(+used || 0));
}

/** Bar color tokens by usage percent. Mirrors STYLE.md §11 (kernel-shared threshold). */
export function barColorByUsage(pct: number): string {
  return thresholdColorToken(pct);
}

export interface InvoiceTx {
  cardId?: string | null;
  accountId?: string | null;
  date?: string | null;
  type?: string | null;
  amount?: number | null;
}

/** Total invoice = sum of |amount| over EXPENSE transactions inside the invoice cycle posted
 * against this card (matched by `tx.cardId`, not the account). O ciclo é da conta, por isso ela
 * entra na assinatura. */
export function invoiceTotal(transactions: readonly InvoiceTx[] | null | undefined, cardId: string, account: Account | null | undefined, period: Period): number {
  const b = invoicePeriod(account, period);
  const cid = String(cardId);
  return (transactions || []).reduce((acc, t) => {
    if (String(t.cardId) !== cid) return acc;
    const dStr = String(t.date || '').slice(0, 10);
    if (dStr < b.from || dStr > b.to) return acc;
    const isExpense = String(t.type || '').toUpperCase() === 'EXPENSE' || (+(t.amount ?? 0) || 0) < 0;
    if (!isExpense) return acc;
    return acc + Math.abs(+(t.amount ?? 0) || 0);
  }, 0);
}

/** Total invoice for every card on the account combined — used for the shared usage bar
 * (account.creditLimit is one limit for all of the account's cards). */
export function accountInvoiceTotal(transactions: readonly InvoiceTx[] | null | undefined, account: Account | null | undefined, period: Period): number {
  const b = invoicePeriod(account, period);
  const aid = String(account?.id);
  return (transactions || []).reduce((acc, t) => {
    if (String(t.accountId) !== aid || t.cardId == null) return acc;
    const dStr = String(t.date || '').slice(0, 10);
    if (dStr < b.from || dStr > b.to) return acc;
    const isExpense = String(t.type || '').toUpperCase() === 'EXPENSE' || (+(t.amount ?? 0) || 0) < 0;
    if (!isExpense) return acc;
    return acc + Math.abs(+(t.amount ?? 0) || 0);
  }, 0);
}

import type { TransactionStatus, TransactionType } from '@/api/overrides.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';

export const TRANSACTION_TYPES = {
  INCOME: 'income',
  EXPENSE: 'expense',
  TRANSFER: 'transfer',
} as const satisfies Record<string, TransactionType>;

export const TRANSACTION_STATUS = {
  PENDING: 'pending',
  SCHEDULED: 'scheduled',
  CONFIRMED: 'confirmed',
} as const satisfies Record<string, TransactionStatus>;

export const GROUP_SCOPE = { SINGLE: 'SINGLE', FUTURE: 'FUTURE', ALL: 'ALL' } as const;
export type GroupScope = (typeof GROUP_SCOPE)[keyof typeof GROUP_SCOPE];

export type StatusBadgeVariant = 'income' | 'warning' | 'info' | 'muted' | 'expense';

/** Forma mínima o bastante para as regras abaixo — o record completo (`TransactionResponse`) é
 * consumido pelas fatias (Fase 6), não aqui. */
export interface TransactionLike {
  type?: unknown;
  status?: unknown;
  description?: string;
  totalInstallments?: number | null;
  installmentNumber?: number | null;
  date?: string | null;
}

export function normalizeType(value: unknown): TransactionType {
  const s = String(value ?? '').toLowerCase();
  if (s === 'income' || s === 'revenue') return TRANSACTION_TYPES.INCOME;
  if (s === 'transfer') return TRANSACTION_TYPES.TRANSFER;
  return TRANSACTION_TYPES.EXPENSE;
}

export function isExpense(t: TransactionLike | null | undefined): boolean {
  return normalizeType(t && t.type) === TRANSACTION_TYPES.EXPENSE;
}
export function isIncome(t: TransactionLike | null | undefined): boolean {
  return normalizeType(t && t.type) === TRANSACTION_TYPES.INCOME;
}
export function isTransfer(t: TransactionLike | null | undefined): boolean {
  return normalizeType(t && t.type) === TRANSACTION_TYPES.TRANSFER;
}

/** Signed amount: expense → negative; income/transfer → positive. */
export function signedAmount(type: unknown, amount: number | string | null | undefined): number {
  const v = Math.abs(+(amount ?? 0) || 0);
  return normalizeType(type) === TRANSACTION_TYPES.EXPENSE ? -v : v;
}

export function statusBadgeVariant(status: unknown): StatusBadgeVariant {
  const s = String(status || '').toLowerCase();
  if (s === TRANSACTION_STATUS.CONFIRMED) return 'income';
  if (s === TRANSACTION_STATUS.SCHEDULED) return 'warning';
  if (s === 'cancelled') return 'muted'; // accounts-payable domain still uses this status
  return 'expense'; // pending or unknown
}

const STATUS_LABEL: Record<string, string> = {
  confirmed: 'Confirmado',
  pending: 'Pendente',
  scheduled: 'Agendado',
};

export function statusLabel(status: unknown): string {
  const s = String(status || '').toLowerCase();
  return STATUS_LABEL[s] || String(status);
}

/** Transfer rule: source account differs from destination. */
export function isValidTransfer(srcAccountId: string | null | undefined, dstAccountId: string | null | undefined): boolean {
  if (srcAccountId == null || dstAccountId == null) return false;
  return String(srcAccountId) !== String(dstAccountId);
}

/** Descrição para EXIBIÇÃO: acrescenta "(n de N)" quando o lançamento é parcela de uma compra
 * parcelada. O sufixo é só visual — a descrição persistida continua limpa de propósito, porque
 * f006.GroupSignature deriva o groupId do hash da descrição normalizada: gravar o sufixo faria o
 * re-import do mesmo extrato calcular outro id e duplicar a compra em vez de reconhecê-la.
 * Lançamento à vista tem totalInstallments = 1 (default do backend), então não recebe sufixo.
 * Transferência reaproveita installmentNumber/totalInstallments só pra ligar as duas pernas entre
 * si (não é parcela de compra nenhuma) — mas essa função não enxerga categoria (não tem o
 * catálogo), então quem chama precisa filtrar transferência ANTES de chamar `describe`
 * (`Category.isTransferCategory`, não `t.type`: o backend nunca manda `type: "transfer"`, só
 * EXPENSE/INCOME — a perna é identificada pela categoria de sistema "Transferência"). */
export function describe(t: TransactionLike | null | undefined): string {
  if (!t) return '';
  const base = t.description || '';
  const total = +(t.totalInstallments ?? 0) || 0;
  const number = +(t.installmentNumber ?? 0) || 0;
  if (total <= 1 || number <= 0) return base;
  return base + ' (' + number + ' de ' + total + ')';
}

export function isToday(t: TransactionLike, now?: Date): boolean {
  const ref = now || new Date();
  const d = new Date(t.date as string);
  return d.getFullYear() === ref.getFullYear() && d.getMonth() === ref.getMonth() && d.getDate() === ref.getDate();
}

export function isInPeriod(t: TransactionLike, period: Period.Period): boolean {
  return Period.containsDate(period, t.date as string);
}

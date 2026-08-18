/** "A Pagar / A Receber" como filtro de transações pendentes. Não há recurso de payables: A
 * Pagar = despesas pendentes; A Receber = receitas pendentes. Sem secondary própria: lê via
 * TransactionsApi (fatia irmã, injetada como porta). */
import type { Period } from '../../core/kernel/_0_domain/period.ts';
import * as PayableDomain from './domain.ts';
import type { PayableTotals } from './domain.ts';

interface PayableTxLike {
  id?: string;
  description?: string;
  date?: string | null;
  amount?: number | string;
  accountId?: string;
  categoryId?: string;
  status?: string;
  groupId?: string | null;
  totalInstallments?: number | null;
  installmentNumber?: number | null;
}

/** Formato que `adapt()` realmente produz — não é `Payable` de `domain.ts` (esse vem de
 * `normalize()`, nunca chamado no app real). Nota: o campo é `description`, não `name` — o
 * mesmo nome que `Domain.Payable.normalize()` usa. O render original (`page.js`) lê
 * `item.name`, que nunca existe aqui: todo título de linha cai no fallback "—". É um bug
 * pré-existente no `web/` original, preservado de propósito (regra de desempate do plano). */
export interface PayableListItem {
  id: string;
  description?: string;
  due: string | null;
  amount: number;
  accountId?: string;
  categoryId?: string;
  status: string;
  type: string;
  groupId?: string | null;
  totalInstallments?: number | null;
  installmentNumber?: number | null;
}

export interface PayableTxRepoPort {
  list(query: string): Promise<PayableTxLike[] | null>;
}

export interface PayableServiceDeps {
  repo: PayableTxRepoPort;
}

export interface PayableService {
  listPayable(): Promise<PayableListItem[]>;
  listReceivable(): Promise<PayableListItem[]>;
  periodTotals(items: readonly PayableListItem[], period: Period): PayableTotals;
  inPeriod(items: readonly PayableListItem[], period: Period): PayableListItem[];
}

// Adapta a transação ao formato consumido pela tela (Domain.Payable): due, amount positivo, type label.
function adapt(label: string) {
  return (txs: PayableTxLike[] | null): PayableListItem[] =>
    (Array.isArray(txs) ? txs : []).map((t) => ({
      id: t.id as string,
      description: t.description,
      due: t.date || null,
      amount: Math.abs(+(t.amount ?? 0) || 0),
      accountId: t.accountId,
      categoryId: t.categoryId,
      status: t.status || 'pending',
      type: label,
      groupId: t.groupId,
      totalInstallments: t.totalInstallments,
      installmentNumber: t.installmentNumber,
    }));
}

export function createPayableService(deps: PayableServiceDeps): PayableService {
  return {
    listPayable: () => deps.repo.list('status=pending&type=expense').then(adapt('PAYABLE')),
    listReceivable: () => deps.repo.list('status=pending&type=income').then(adapt('RECEIVABLE')),
    periodTotals: (items, period) => PayableDomain.periodTotals(items, period),
    inPeriod: (items, period) => PayableDomain.inPeriod(items, period),
  };
}

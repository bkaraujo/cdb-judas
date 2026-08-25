export const STATEMENT_ITEM_STATUS = { BALANCE: 'balance', CONFIRMED: 'confirmed' } as const;

export interface StatementSourceTx {
  id: string;
  date: string;
  purchaseDate?: string | null;
  description: string;
  amount: number;
  status: string;
  categoryId: string | null;
  cardId?: string | null;
  invoice?: boolean;
  installmentNumber?: number | null;
  totalInstallments?: number | null;
  tagIds?: string[];
}

export interface StatementRow {
  id: string | null;
  date: string | null;
  purchaseDate?: string | null;
  description: string;
  amount: number;
  status: string;
  runningBal: number;
  categoryId: string | null;
  cardId?: string | null;
  invoice?: boolean;
  installmentNumber?: number | null;
  totalInstallments?: number | null;
  tagIds?: string[];
}

export interface BuildRowsOptions {
  headerLabel?: string;
  headerBalance?: number;
  startBalance?: number;
}

export function isBalanceHeader(rowOrStatus: StatementRow | string | null | undefined): boolean {
  const s = typeof rowOrStatus === 'string' ? rowOrStatus : rowOrStatus && rowOrStatus.status;
  return String(s || '').toLowerCase() === STATEMENT_ITEM_STATUS.BALANCE;
}

/** Running balance de uma linha construída por `buildRows` — sempre carrega `runningBal`; o único
 * chamador (statement-row.ts, Fase 5) nunca passa outra coisa. */
export function runningBalance(row: StatementRow | null | undefined): number {
  return row ? +row.runningBal || 0 : 0;
}

/** Builds the statement rows for a period: a "Saldo anterior" balance-header row carrying
 * `opening`, then each transaction (all statuses, sorted ascending by date) with its cumulative
 * running balance. Empty `txs` yields the opening row alone. Pure — no I/O. `opts` (opcional)
 * atende o extrato de cartão, onde o cabeçalho mostra o total da fatura ANTERIOR mas o acumulado
 * do ciclo atual recomeça do zero: { headerLabel: 'Fatura anterior', headerBalance: <total
 * anterior>, startBalance: 0 } */
export function buildRows(
  opening: number,
  txs: readonly StatementSourceTx[] | null | undefined,
  openingDate: string | null | undefined,
  opts?: BuildRowsOptions,
): StatementRow[] {
  const o = opts || {};
  const open = +opening || 0;
  const headerBalance = o.headerBalance != null ? +o.headerBalance : open;
  const rows: StatementRow[] = [
    {
      id: null,
      date: openingDate || null,
      description: o.headerLabel || 'Saldo anterior',
      amount: 0,
      status: STATEMENT_ITEM_STATUS.BALANCE,
      runningBal: headerBalance,
      categoryId: null,
    },
  ];
  let running = o.startBalance != null ? +o.startBalance : open;
  (txs || [])
    .slice()
    .sort((a, b) => String(a.date).localeCompare(String(b.date)))
    .forEach((t) => {
      running += +t.amount || 0;
      rows.push({
        id: t.id,
        date: t.date,
        // Data da 1ª parcela do grupo (fixa) — a view do extrato mostra ela no lugar da data
        // desta parcela quando a linha é parcelada; `date` segue mandando no bucketing.
        purchaseDate: t.purchaseDate != null ? t.purchaseDate : null,
        description: t.description,
        amount: +t.amount || 0,
        status: t.status,
        runningBal: running,
        categoryId: t.categoryId,
        // Propagados para a view: `invoice` marca a linha derivada de cartões (statement.js
        // esconde as ações e linka #/credit-cards); `cardId` resolve o cartão da compra.
        cardId: t.cardId != null ? t.cardId : null,
        invoice: t.invoice === true,
        // Parcela: alimenta Domain.Transaction.describe na linha do extrato.
        installmentNumber: t.installmentNumber != null ? t.installmentNumber : null,
        totalInstallments: t.totalInstallments != null ? t.totalInstallments : null,
        // Vínculos de tag: alimentam o indicador de tags da linha (UI.tagFlagHtml).
        tagIds: Array.isArray(t.tagIds) ? t.tagIds : [],
      });
    });
  return rows;
}

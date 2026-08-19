/** Contrato compartilhado pelos painéis do Dashboard: `window.DashboardPanels` (tabela de
 * dispatch string→renderer do original) vira `Record<PanelId, PanelRenderer>` (ver `./index.ts`),
 * cada painel um `(p, ctx) => JQuery` puro dado o `ctx` — sem estado próprio, sem import de
 * fatia irmã (as poucas APIs cross-slice que os painéis precisam — cartões, metas, contas a
 * pagar/receber — chegam via `ctx`, injetadas por `page.ts`). */
import type { TransactionResponse } from '@/api/overrides.ts';
import type { Account } from '@/core/kernel/_0_domain/account.ts';
import type { AccountsPayableApi } from '@/feature/accounts-payable/api.ts';
import type { BudgetApi } from '@/feature/budget/api.ts';
import type { CreditCardsApi } from '@/feature/credit-cards/api.ts';
import type { MonthlyBucket } from '@/feature/dashboard/domain.ts';

export type PayableListItem = Awaited<ReturnType<AccountsPayableApi['listPayable']>>[number];

/** `MonthlyBucket` com `month` já formatado como rótulo curto ("Jan", "Fev"...) pro eixo do
 * mini-gráfico — não dá pra usar interseção (`month` seria `number & string`, impossível). */
export type LabeledBucket = Omit<MonthlyBucket, 'month'> & { month: string };

export type PanelId =
  | 'cash-balances'
  | 'month-result'
  | 'expenses-cat'
  | 'revenues-cat'
  | 'accounts-payable'
  | 'accounts-receivable'
  | 'credit-cards'
  | 'cash-flow'
  | 'expense-goals'
  | 'recent-postings'
  | 'balance-sheet';

export interface PanelDef {
  id: PanelId;
  label: string;
  defaultOn: boolean;
  icon: string;
}

export interface PanelWrapOptions {
  title: string;
  icon?: string;
  action?: JQuery | string;
  body: JQuery | string;
}

export interface DashboardData {
  transactions: TransactionResponse[];
  payables: PayableListItem[];
  receivables: PayableListItem[];
  loaded: boolean;
}

export interface DashboardCtxState {
  data: DashboardData;
  hideValues: boolean;
  settings: { scrollPanels: boolean };
}

export interface CategoryBarDatum {
  name: string;
  amount: number;
  color: string;
}

/** Utilitários compartilhados que `page.ts` monta a cada render e repassa aos painéis — mesmo
 * papel do `buildCtx()` original. */
export interface PanelCtx {
  state: DashboardCtxState;
  panelWrap(opts: PanelWrapOptions): JQuery;
  v(n: number): string;
  esc(s: unknown): string;
  pickColor(i: number, fallback?: string | null): string;
  cashAccounts(): Account[];
  creditCards(): Account[];
  cbdAccounts(): Account[];
  currentMonthTxs(): TransactionResponse[];
  allTxs(): TransactionResponse[];
  categoryName(catId: string | null | undefined): string;
  isTransferCategory(catId: string | null | undefined): boolean;
  txIsExpense(t: TransactionResponse): boolean;
  expenseByCategory(): CategoryBarDatum[];
  monthlySeries(nMonths: number): LabeledBucket[];
  upcomingPayables(typ: 'expense' | 'income'): PayableListItem[];
  categoryBars(data: readonly CategoryBarDatum[]): string;
  miniLineChart(data: readonly LabeledBucket[], height?: number): string;
  stubPanel(p: PanelDef): JQuery;
  SERIES_PALETTE: readonly string[];
  creditCardsApi: CreditCardsApi;
  budgetApi: BudgetApi;
}

export type PanelRenderer = (p: PanelDef, ctx: PanelCtx) => JQuery;

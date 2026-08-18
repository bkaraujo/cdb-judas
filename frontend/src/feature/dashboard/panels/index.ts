/** `window.DashboardPanels` (tabela de dispatch string→renderer, 11 sites de definição no
 * original) vira este `Record<PanelId, PanelRenderer>`. */
import { accountsPayable, accountsReceivable } from './accounts-payable.ts';
import { balanceSheet } from './balance-sheet.ts';
import { cashBalances } from './cash-balances.ts';
import { cashFlow } from './cash-flow.ts';
import { creditCards } from './credit-cards.ts';
import { expenseGoals } from './expense-goals.ts';
import { expensesByCategory } from './expenses-by-category.ts';
import { monthResult } from './month-result.ts';
import { recentPostings } from './recent-postings.ts';
import { revenuesByCategory } from './revenues-by-category.ts';
import type { PanelId, PanelRenderer } from './types.ts';

export const PANELS: Record<PanelId, PanelRenderer> = {
  'cash-balances': cashBalances,
  'month-result': monthResult,
  'expenses-cat': expensesByCategory,
  'revenues-cat': revenuesByCategory,
  'accounts-payable': accountsPayable,
  'accounts-receivable': accountsReceivable,
  'credit-cards': creditCards,
  'cash-flow': cashFlow,
  'expense-goals': expenseGoals,
  'recent-postings': recentPostings,
  'balance-sheet': balanceSheet,
};

export type { PanelCtx, PanelDef, PanelId, PanelRenderer, PayableListItem } from './types.ts';

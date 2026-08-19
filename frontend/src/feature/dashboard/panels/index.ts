/** `window.DashboardPanels` (tabela de dispatch string→renderer, 11 sites de definição no
 * original) vira este `Record<PanelId, PanelRenderer>`. */
import { accountsPayable, accountsReceivable } from '@/feature/dashboard/panels/accounts-payable.ts';
import { balanceSheet } from '@/feature/dashboard/panels/balance-sheet.ts';
import { cashBalances } from '@/feature/dashboard/panels/cash-balances.ts';
import { cashFlow } from '@/feature/dashboard/panels/cash-flow.ts';
import { creditCards } from '@/feature/dashboard/panels/credit-cards.ts';
import { expenseGoals } from '@/feature/dashboard/panels/expense-goals.ts';
import { expensesByCategory } from '@/feature/dashboard/panels/expenses-by-category.ts';
import { monthResult } from '@/feature/dashboard/panels/month-result.ts';
import { recentPostings } from '@/feature/dashboard/panels/recent-postings.ts';
import { revenuesByCategory } from '@/feature/dashboard/panels/revenues-by-category.ts';
import type { PanelId, PanelRenderer } from '@/feature/dashboard/panels/types.ts';

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

export type { PanelCtx, PanelDef, PanelId, PanelRenderer, PayableListItem } from '@/feature/dashboard/panels/types.ts';

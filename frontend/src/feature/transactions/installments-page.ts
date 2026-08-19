/** pages/installments — Parcelamentos: mesma fábrica de `page.ts` (Lançamentos), só muda o
 * título e o filtro extra de totalInstallments > 1 (ver installmentsOnly). */
import { createTransactionsListPage } from '@/feature/transactions/page.ts';
import type { Page } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import type { TransactionsPageDeps } from '@/feature/transactions/page.ts';

export function createInstallmentsPage(deps: TransactionsPageDeps): Page {
  return createTransactionsListPage(deps, { title: 'Parcelamentos', installmentsOnly: true });
}

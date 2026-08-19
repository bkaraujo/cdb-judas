/** Contrato público da fatia credit-cards (equivalente ao FNNNApi do backend). Único arquivo que
 * outra fatia pode referenciar. Consumidor: dashboard (painel de cartões de crédito). */
import type { Account } from '@/core/kernel/_0_domain/account.ts';
import type { Period } from '@/core/kernel/_0_domain/period.ts';
import * as CreditCardDomain from '@/feature/credit-cards/domain.ts';
import type { InvoiceTx } from '@/feature/credit-cards/domain.ts';
import type { CreditCardService } from '@/feature/credit-cards/service.ts';

export interface CreditCardsApi {
  accountsWithCards(): Account[];
  usagePct(used: number, limit: number): number;
  barColorByUsage(pct: number): string;
  accountInvoiceTotal(transactions: readonly InvoiceTx[] | null | undefined, account: Account | null | undefined, period: Period): number;
}

export function createCreditCardsApi(service: CreditCardService): CreditCardsApi {
  return {
    accountsWithCards: () => service.accountsWithCards(),
    usagePct: (used, limit) => CreditCardDomain.usagePct(used, limit),
    barColorByUsage: (pct) => CreditCardDomain.barColorByUsage(pct),
    accountInvoiceTotal: (transactions, account, period) => CreditCardDomain.accountInvoiceTotal(transactions, account, period),
  };
}

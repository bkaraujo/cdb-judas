/** Credit card use cases. Sem secondary própria: lê via TransactionsApi (fatia irmã, injetada
 * como porta — transactions vem depois na ordem do plano) e via CacheStore (contas + cartões). */
import type { Account, Card } from '../../core/kernel/_0_domain/account.ts';
import * as AccountDomain from '../../core/kernel/_0_domain/account.ts';
import { cycleFor, dueDatesIn } from '../../core/kernel/_0_domain/invoice.ts';
import type { Period } from '../../core/kernel/_0_domain/period.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import * as CreditCardDomain from './domain.ts';
import type { InvoiceTx } from './domain.ts';

export interface CreditCardWithAccount extends Card {
  account: Account;
}

/** Porta mínima do repositório de transações (`transactions/repository.ts`, fatia irmã que vem
 * depois na ordem do plano) — só o que esta fatia precisa. */
export interface CreditCardsTxRepoPort {
  listByAccount(accountId: string, query: string): Promise<InvoiceTx[] | null>;
}

export interface CreditCardServiceDeps {
  txRepo: CreditCardsTxRepoPort;
  cache: CacheStore;
}

export interface CreditCardInvoice {
  dueDate: string | null;
  cycle: { from: string; to: string } | null;
  transactions: InvoiceTx[];
  total: number;
}

export interface CreditCardService {
  /** Every card across every account, flattened, each carrying its owning account (for
   * color/name/limit) — used by views that group cards by account client-side. */
  listFromCache(): CreditCardWithAccount[];
  /** Accounts that have at least one card — used by views that render one row/bar per account
   * rather than per card (e.g. the dashboard panel). */
  accountsWithCards(): Account[];
  /** Fatura que VENCE em `period`: as compras do ciclo (que começa no mês anterior — ver
   * Domain.Invoice) lançadas contra este cartão. Devolve também o vencimento e o ciclo, que a
   * tela de Extrato do Cartão usa no cabeçalho. Período sem vencimento → fatura vazia. */
  invoiceFor(card: Card, account: Account, period: Period): Promise<CreditCardInvoice>;
}

export function createCreditCardService(deps: CreditCardServiceDeps): CreditCardService {
  return {
    listFromCache: () => {
      const out: CreditCardWithAccount[] = [];
      deps.cache.accounts().forEach((a) => {
        (a.cards || []).forEach((c) => {
          out.push({ ...c, account: a });
        });
      });
      return out;
    },
    accountsWithCards: () => deps.cache.accounts().filter(AccountDomain.hasCards),
    invoiceFor: (card, account, period) => {
      const dues = dueDatesIn(account, period);
      if (!dues.length) {
        return Promise.resolve({ dueDate: null, cycle: null, transactions: [], total: 0 });
      }
      const due = dues[0] as string;
      const cycle = cycleFor(account, due);
      return deps.txRepo.listByAccount(account.id, 'dateFrom=' + cycle.from + '&dateTo=' + cycle.to).then((txs) => {
        const items = (Array.isArray(txs) ? txs : []).filter((t) => String(t.cardId) === String(card.id));
        return {
          dueDate: due,
          cycle,
          transactions: items,
          total: CreditCardDomain.invoiceTotal(items, card.id, account, period),
        };
      });
    },
  };
}

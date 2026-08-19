/** Extrato orchestration. Assembles statement rows client-side from the existing transactions +
 * monthly-balance endpoints (no /statements backend). Sem domain própria e sem secondary: lê via
 * TransactionsApi/AccountsApi (fatias irmãs, injetadas como porta), nunca repositório cru. */
import type { BalanceResponse } from '@/api/types.ts';
import type { Account } from '@/core/kernel/_0_domain/account.ts';
import * as AccountDomain from '@/core/kernel/_0_domain/account.ts';
import { collapse, fetchWindow, mergeCards } from '@/core/kernel/_0_domain/invoice.ts';
import type { InvoiceTx } from '@/core/kernel/_0_domain/invoice.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as StatementItem from '@/core/kernel/_0_domain/statement-item.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';

export interface StatementTxPort {
  listByAccount(accountId: string, query: string): Promise<InvoiceTx[] | null>;
}

export interface StatementBalancePort {
  allAccounts(period: Period.Period): Promise<BalanceResponse[] | null>;
}

export interface StatementServiceDeps {
  txRepo: StatementTxPort;
  balance: StatementBalancePort;
  cache: CacheStore;
}

export interface AccountStatementSummary {
  accountId: string;
  closingBalance: number;
  openingBalance: number;
}

export interface LoadStatementResult {
  rows: StatementItem.StatementRow[];
  /** Resposta crua da conta na janela alargada (superset do mês, fetchWindow) — é o índice que a
   * página usa pra resolver o lançamento completo por trás de uma linha clicada (findFullTx),
   * sem precisar de um segundo GET nem de um índice global de todas as contas. */
  raw: InvoiceTx[];
}

export interface StatementService {
  /** Detail for one account in a period: a "Saldo anterior" row carrying the opening balance
   * (previous period's closing, resolved by summary() — no balance fetch here), then each
   * transaction (all statuses) with its running balance. Compras de cartão não entram linha a
   * linha nem cartão a cartão: Domain.Invoice.collapse as troca por uma linha por (cartão,
   * vencimento) e Domain.Invoice.mergeCards funde os cartões da conta numa linha 'Cartões de
   * crédito' por vencimento. */
  load(accountId: string, period: Period.Period, openingBalance: number): Promise<LoadStatementResult>;
  /** Left-column panorama: closing + opening (previous period's closing) balance per checking
   * account for the period. Accounts absent from a batch response (no snapshot yet) fall back to
   * the account's current/initial balance. */
  summary(period: Period.Period): Promise<AccountStatementSummary[]>;
}

export function createStatementService(deps: StatementServiceDeps): StatementService {
  function checkings(): Account[] {
    return deps.cache.accounts();
  }

  return {
    load: (accountId, period, openingBalance) => {
      const account = deps.cache.findById('accounts', accountId);
      if (!account) return Promise.resolve({ rows: [], raw: [] });
      const b = Period.bounds(period);
      const w = fetchWindow([account], period);
      return deps.txRepo.listByAccount(accountId, 'dateFrom=' + w.from + '&dateTo=' + w.to).then((txs) => {
        const raw = Array.isArray(txs) ? txs : [];
        const rows = collapse(raw, [account], period);
        return { rows: StatementItem.buildRows(openingBalance, mergeCards(rows) as StatementItem.StatementSourceTx[], b.from), raw };
      });
    },
    summary: (period) => {
      const prevPeriod = Period.shift(period, -1);
      function byAccountId(list: BalanceResponse[] | null): Record<string, number> {
        const map: Record<string, number> = {};
        (Array.isArray(list) ? list : []).forEach((b) => {
          map[String(b.accountId)] = +(b.balance ?? 0) || 0;
        });
        return map;
      }
      return Promise.all([deps.balance.allAccounts(period), deps.balance.allAccounts(prevPeriod)]).then(([closing, opening]) => {
        const closingByAccount = byAccountId(closing);
        const openingByAccount = byAccountId(opening);
        return checkings().map((a) => {
          const id = String(a.id);
          return {
            accountId: a.id,
            closingBalance: id in closingByAccount ? (closingByAccount[id] as number) : AccountDomain.currentBalance(a),
            openingBalance: id in openingByAccount ? (openingByAccount[id] as number) : +a.balance || 0,
          };
        });
      });
    },
  };
}

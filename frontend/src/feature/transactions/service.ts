/** Transaction use cases. Sem domain própria: Domain.Transaction é kernel
 * (core/kernel/_0_domain/transaction.ts) — statement/credit-cards/accounts-payable precisam da
 * forma pura pra renderizar linha de extrato/fatura/conta a pagar. */
import type {ImportPreview, PatchStatusRequest, TransactionRequest, TransactionResponse} from '@/api/overrides.ts';
import type {TransferRequest} from '@/api/types.ts';
import type {Account} from '@/core/kernel/_0_domain/account.ts';
import type {CollapsedRow, InvoiceTx} from '@/core/kernel/_0_domain/invoice.ts';
import * as Invoice from '@/core/kernel/_0_domain/invoice.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as Transaction from '@/core/kernel/_0_domain/transaction.ts';
import type {CacheStore} from '@/core/kernel/_1_application/cache-store.ts';
import type {TransactionRepository} from '@/feature/transactions/repository.ts';

export interface TransactionServiceDeps {
  repo: TransactionRepository;
  cache: CacheStore;
}

export interface ListForPeriodResult {
  rows: CollapsedRow[];
  raw: TransactionResponse[];
}

export interface ListInstallmentsResult {
  rows: TransactionResponse[];
  raw: TransactionResponse[];
}

export interface TransactionService {
  list(params?: string): Promise<TransactionResponse[] | null>;
  /** Lançamentos do período com as compras de cartão colapsadas em UMA linha 'Cartões de
   * crédito' por vencimento (collapse + mergeCards — ver Domain.Invoice). A busca usa a janela
   * alargada (o ciclo da fatura começa no mês anterior), por isso `raw` traz mais do que o
   * período: é o índice cru que os modais de editar/excluir usam para resolver a transação por
   * trás de uma linha. */
  listForPeriod(period: Period.Period): Promise<ListForPeriodResult>;
  /** Compras parceladas do período (tela Parcelamentos): mesma janela alargada de
   * listForPeriod, mas SEM collapse — cada parcela aparece como linha própria, reposicionada pro
   * mês de VENCIMENTO da fatura (mesma âncora de exibição usada em toda a tela de cartão). `rows`
   * traz cópias com a data ajustada só para exibição/ordenação; `raw` mantém a data original. */
  listInstallmentsForPeriod(period: Period.Period): Promise<ListInstallmentsResult>;
  listByAccount(accountId: string, params?: string): Promise<TransactionResponse[] | null>;
  create(data: TransactionRequest): Promise<TransactionResponse | null>;
  update(id: string, data: TransactionRequest): Promise<TransactionResponse | null>;
  patchStatus(accountId: string, id: string, status: PatchStatusRequest['status'], date: string): Promise<TransactionResponse | null>;
  remove(accountId: string, id: string, scope?: string): Promise<void | null>;
  transfer(data: TransferRequest): Promise<TransactionResponse | null>;
  importPreview(file: File, password: string | undefined, accountId: string | undefined): Promise<ImportPreview | null>;
  importConfirm(data: unknown): Promise<unknown>;
  /** Builds payload with the domain signing rule applied to amount. */
  buildPayload<T extends { type: unknown; amount: number | string }>(form: T): Omit<T, 'type' | 'amount'> & { type: string; amount: number };
}

export function createTransactionService(deps: TransactionServiceDeps): TransactionService {
  return {
    list: (params) => deps.repo.list(params),
    listForPeriod: (period) => {
      const accounts = deps.cache.accounts();
      const w = Invoice.fetchWindow(accounts, period);
      return deps.repo.list('dateFrom=' + w.from + '&dateTo=' + w.to).then((list) => {
        const raw = Array.isArray(list) ? list : [];
        const rows = Invoice.collapse(raw as InvoiceTx[], accounts, period);
        return { rows: Invoice.mergeCards(rows, accounts), raw };
      });
    },
    listInstallmentsForPeriod: (period) => {
      const accounts = deps.cache.accounts();
      const w = Invoice.fetchWindow(accounts, period);
      const bounds = Period.bounds(period);
      const cardIndex: Record<string, Account> = {};
      accounts.forEach((a) => {
        (a.cards || []).forEach((c) => {
          cardIndex[String(c.id)] = a;
        });
      });
      return deps.repo.list('dateFrom=' + w.from + '&dateTo=' + w.to).then((list) => {
        const raw = Array.isArray(list) ? list : [];
        const rows = raw
          .map((t) => {
            const acc = t.cardId != null ? cardIndex[String(t.cardId)] : null;
            return acc ? { ...t, date: Invoice.dueDate(acc, t.date as string) } : t;
          })
          .filter((t) => (t.date as string) >= bounds.from && (t.date as string) <= bounds.to);
        return { rows, raw };
      });
    },
    listByAccount: (accountId, params) => deps.repo.listByAccount(accountId, params),
    create: (data) => deps.repo.create(data),
    update: (id, data) => deps.repo.update(id, data),
    patchStatus: (accountId, id, status, date) => deps.repo.patchStatus(accountId, id, status, date),
    remove: (accountId, id, scope) => deps.repo.remove(accountId, id, scope),
    transfer: (data) => deps.repo.transfer(data),
    importPreview: (file, password, accountId) => deps.repo.importPreview(file, password, accountId),
    importConfirm: (data) => deps.repo.importConfirm(data),
    buildPayload: (form) => {
      const type = Transaction.normalizeType(form.type);
      return { ...form, type, amount: Transaction.signedAmount(type, form.amount) };
    },
  };
}

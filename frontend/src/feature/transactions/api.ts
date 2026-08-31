/** Contrato público da fatia transactions (equivalente ao FNNNApi do backend). Único arquivo que
 * outra fatia pode referenciar. Consumidores: statement, card-statement (editor/exclusão/marcar-
 * pago de uma linha), credit-cards, accounts-payable, dashboard (leitura de transações no lugar
 * do repositório cru). accounts-payable também escreve (uma conta a pagar/receber É uma
 * transação com status pendente/agendado/confirmado). import-statement usa importPreview/
 * importConfirm. */
import type {ImportPreview, PatchStatusRequest, TransactionRequest, TransactionResponse} from '@/api/overrides.ts';
import type {
    MarkPaidOptions,
    OpenDeleteModalOptions,
    OpenFormModalOptions,
    TransactionActions,
    TxLike
} from '@/feature/transactions/actions.ts';
import type {TransactionService} from '@/feature/transactions/service.ts';

export interface TransactionsApi {
  list(params?: string): Promise<TransactionResponse[] | null>;
  listByAccount(accountId: string, params?: string): Promise<TransactionResponse[] | null>;
  create(data: TransactionRequest): Promise<TransactionResponse | null>;
  update(id: string, data: TransactionRequest): Promise<TransactionResponse | null>;
  remove(accountId: string, id: string, scope?: string): Promise<void | null>;
  patchStatus(accountId: string, id: string, status: PatchStatusRequest['status'], date: string): Promise<TransactionResponse | null>;
  openEditor(opts: OpenFormModalOptions): void;
  openDeleteFlow(tx: TxLike, opts?: OpenDeleteModalOptions): void;
  markPaid(tx: TxLike, opts?: MarkPaidOptions): void;
  importPreview(file: File, password: string | undefined, accountId: string | undefined): Promise<ImportPreview | null>;
  importConfirm(payload: unknown): Promise<unknown>;
}

export function createTransactionsApi(service: TransactionService, actions: TransactionActions): TransactionsApi {
  return {
    list: (params) => service.list(params),
    listByAccount: (accountId, params) => service.listByAccount(accountId, params),
    create: (data) => service.create(data),
    update: (id, data) => service.update(id, data),
    remove: (accountId, id, scope) => service.remove(accountId, id, scope),
    patchStatus: (accountId, id, status, date) => service.patchStatus(accountId, id, status, date),
    openEditor: (opts) => actions.openFormModal(opts),
    openDeleteFlow: (tx, opts) => actions.openDeleteModal(tx, opts),
    markPaid: (tx, opts) => actions.markPaid(tx, opts),
    importPreview: (file, password, accountId) => service.importPreview(file, password, accountId),
    importConfirm: (payload) => service.importConfirm(payload),
  };
}

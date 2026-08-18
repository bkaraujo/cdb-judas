/** HTTP adapter for transactions under the accounts namespace: /api/{uuid}/accounts/... */
import type { ImportPreview, PatchStatusRequest, TransactionRequest, TransactionResponse } from '../../api/overrides.ts';
import type { TransferRequest } from '../../api/types.ts';
import type { HttpClient } from '../../core/kernel/_2_infrastructure/secondary/http-client.ts';

export interface TransactionRepository {
  list(params?: string): Promise<TransactionResponse[] | null>;
  listByAccount(accountId: string, params?: string): Promise<TransactionResponse[] | null>;
  create(data: TransactionRequest): Promise<TransactionResponse | null>;
  update(id: string, data: TransactionRequest): Promise<TransactionResponse | null>;
  patchStatus(accountId: string, id: string, status: PatchStatusRequest['status'], paymentDate: string): Promise<TransactionResponse | null>;
  remove(accountId: string, id: string, mode?: string): Promise<void | null>;
  transfer(data: TransferRequest): Promise<TransactionResponse | null>;
  importPreview(file: File, password: string | undefined, accountId: string | undefined): Promise<ImportPreview | null>;
  importConfirm(data: unknown): Promise<unknown>;
}

export function createTransactionRepository(http: HttpClient): TransactionRepository {
  return {
    list: (params) => http.get<TransactionResponse[]>('/accounts/transactions' + (params ? '?' + params : '')),
    listByAccount: (accountId, params) => http.get<TransactionResponse[]>('/accounts/' + accountId + '/transactions' + (params ? '?' + params : '')),
    create: (data) => http.post<TransactionResponse>('/accounts/' + data.accountId + '/transactions', data),
    update: (id, data) => http.patch<TransactionResponse>('/accounts/' + data.accountId + '/transactions/' + id, data),
    patchStatus: (accountId, id, status, paymentDate) =>
      http.patch<TransactionResponse>('/accounts/' + accountId + '/transactions/' + id + '/status', { status, paymentDate } satisfies PatchStatusRequest),
    remove: (accountId, id, mode) => http.delete('/accounts/' + accountId + '/transactions/' + id + (mode ? '?mode=' + mode : '')),
    transfer: (data) => http.post<TransactionResponse>('/accounts/transactions/transfer', data),
    importPreview: (file, password, accountId) => {
      const fd = new FormData();
      fd.append('file', file);
      if (password) fd.append('password', password);
      if (accountId) fd.append('accountId', accountId);
      return http.upload<ImportPreview>('/accounts/transactions/import/preview', fd);
    },
    importConfirm: (data) => http.post('/accounts/transactions/import/confirm', data),
  };
}

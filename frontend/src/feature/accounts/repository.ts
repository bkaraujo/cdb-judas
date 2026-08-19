/** HTTP adapters for /accounts (+ nested cards), monthly balance snapshot, e /accounts/closing. */
import type { AccountRequest, AccountResponse, BalanceResponse, Card, CardRequest, ClosingRequest, ClosingResponse } from '@/api/types.ts';
import type { DeletionQueryOptions, HttpClient } from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';
import { deletionQuery } from '@/core/kernel/_2_infrastructure/secondary/http-client.ts';

export interface AccountRepository {
  list(): Promise<AccountResponse[] | null>;
  create(data: AccountRequest): Promise<AccountResponse | null>;
  update(id: string, data: AccountRequest): Promise<AccountResponse | null>;
  remove(id: string, opts?: DeletionQueryOptions): Promise<void | null>;
  listCards(accountId: string): Promise<Card[] | null>;
  createCard(accountId: string, data: CardRequest): Promise<Card | null>;
  removeCard(accountId: string, cardId: string, opts?: DeletionQueryOptions): Promise<void | null>;
  setCardActive(accountId: string, cardId: string, active: boolean): Promise<Card | null>;
}

export function createAccountRepository(http: HttpClient): AccountRepository {
  return {
    list: () => http.get<AccountResponse[]>('/accounts'),
    create: (data) => http.post<AccountResponse>('/accounts', data),
    update: (id, data) => http.patch<AccountResponse>('/accounts/' + id, data),
    remove: (id, opts) => http.delete('/accounts/' + id + deletionQuery(opts)),
    listCards: (accountId) => http.get<Card[]>('/accounts/' + accountId + '/cards'),
    createCard: (accountId, data) => http.post<Card>('/accounts/' + accountId + '/cards', data),
    removeCard: (accountId, cardId, opts) => http.delete('/accounts/' + accountId + '/cards/' + cardId + deletionQuery(opts)),
    setCardActive: (accountId, cardId, active) => http.patch<Card>('/accounts/' + accountId + '/cards/' + cardId, { active }),
  };
}

/** Single owner of monthly-balance snapshot access for the frontend. */
export interface BalanceRepository {
  monthly(id: string, yyyyMM: string): Promise<BalanceResponse | null>;
  allAccounts(yyyyMM: string): Promise<BalanceResponse[] | null>;
}

export function createBalanceRepository(http: HttpClient): BalanceRepository {
  return {
    monthly: (id, yyyyMM) => http.get<BalanceResponse>('/accounts/' + id + '/balance?period=' + yyyyMM),
    allAccounts: (yyyyMM) => http.get<BalanceResponse[]>('/accounts/balance?period=' + yyyyMM),
  };
}

export interface ClosingRepository {
  get(): Promise<ClosingResponse | null>;
  set(period: string): Promise<ClosingResponse | null>;
  clear(): Promise<void | null>;
}

export function createClosingRepository(http: HttpClient): ClosingRepository {
  return {
    get: () => http.get<ClosingResponse>('/accounts/closing'),
    set: (period) => http.post<ClosingResponse>('/accounts/closing', { period } satisfies ClosingRequest),
    clear: () => http.delete('/accounts/closing'),
  };
}

/** Account, monthly-balance e accounting-closing use cases. Sem domain própria: Domain.Account é
 * kernel (core/kernel/_0_domain/account.ts) — transactions/credit-cards/statement/dashboard
 * precisam da forma pura (currentBalance/hasCards). */
import type { AccountRequest, AccountResponse, BalanceResponse, Card, CardRequest, ClosingResponse } from '../../api/types.ts';
import type { Account } from '../../core/kernel/_0_domain/account.ts';
import type { Period } from '../../core/kernel/_0_domain/period.ts';
import { yyyymm } from '../../core/kernel/_0_domain/period.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import type { CbdChangeDetail } from '../../core/kernel/_1_application/event-bus.ts';
import type { DeletionQueryOptions } from '../../core/kernel/_2_infrastructure/secondary/http-client.ts';
import type { AccountRepository, BalanceRepository, ClosingRepository } from './repository.ts';

export interface AccountServiceDeps {
  repo: AccountRepository;
  cache: CacheStore;
}

export interface AccountService {
  list(): Promise<AccountResponse[] | null>;
  create(data: AccountRequest): Promise<AccountResponse | null>;
  update(id: string, data: AccountRequest): Promise<AccountResponse | null>;
  remove(id: string, opts?: DeletionQueryOptions): Promise<void | null>;
  /** List from cache (no network) — used by views that need accounts in sync with SSE updates. */
  listCached(): Account[];
  findById(id: string | null | undefined): Account | null;
  addCard(accountId: string, data: CardRequest): Promise<Card | null>;
  removeCard(accountId: string, cardId: string, opts?: DeletionQueryOptions): Promise<void | null>;
  setCardActive(accountId: string, cardId: string, active: boolean): Promise<Card | null>;
  onChange(cb: (detail: CbdChangeDetail) => void): () => void;
}

export function createAccountService(deps: AccountServiceDeps): AccountService {
  return {
    list: () => deps.repo.list(),
    create: (data) => deps.repo.create(data),
    update: (id, data) => deps.repo.update(id, data),
    remove: (id, opts) => deps.repo.remove(id, opts),
    listCached: () => deps.cache.accounts(),
    findById: (id) => deps.cache.findById('accounts', id),
    addCard: (accountId, data) => deps.repo.createCard(accountId, data),
    removeCard: (accountId, cardId, opts) => deps.repo.removeCard(accountId, cardId, opts),
    setCardActive: (accountId, cardId, active) => deps.repo.setCardActive(accountId, cardId, active),
    onChange: (cb) => deps.cache.subscribe('ACCOUNT', cb),
  };
}

export interface BalanceServiceDeps {
  repo: BalanceRepository;
}

export interface BalanceService {
  /** Resolves to the MonthlyBalance { id, accountId, period, balance } for the account/period, or
   * rejects with err.status === 404 when no snapshot exists. */
  monthly(accountId: string, period: Period): Promise<BalanceResponse | null>;
  /** Resolves to BalanceResponse[] for every account of the current user in the given period, in
   * a single request — accounts without a snapshot for the period are simply absent from the
   * list. */
  allAccounts(period: Period): Promise<BalanceResponse[] | null>;
}

export function createBalanceService(deps: BalanceServiceDeps): BalanceService {
  return {
    monthly: (accountId, period) => deps.repo.monthly(accountId, yyyymm(period)),
    allAccounts: (period) => deps.repo.allAccounts(yyyymm(period)),
  };
}

export interface ClosingServiceDeps {
  repo: ClosingRepository;
}

export interface ClosingService {
  get(): Promise<ClosingResponse | null>;
  set(period: string): Promise<ClosingResponse | null>;
  clear(): Promise<void | null>;
}

export function createClosingService(deps: ClosingServiceDeps): ClosingService {
  return {
    get: () => deps.repo.get(),
    set: (period) => deps.repo.set(period),
    clear: () => deps.repo.clear(),
  };
}

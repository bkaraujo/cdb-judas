import { describe, expect, it } from 'vitest';
import type { Account } from '../../core/kernel/_0_domain/account.ts';
import * as Period from '../../core/kernel/_0_domain/period.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import { createStatementService } from './service.ts';
import type { StatementBalancePort, StatementTxPort } from './service.ts';

const ACCOUNT = { id: '1', name: 'Conta', balance: 999, cards: [] } as unknown as Account;

function fakeCache(accounts: Account[], byId: Record<string, Account | null>): CacheStore {
  return {
    accounts: () => accounts,
    findById: ((key: string, id: string) => (key === 'accounts' ? byId[id] || null : null)) as CacheStore['findById'],
  } as unknown as CacheStore;
}

const fakeBalance: StatementBalancePort = {
  // Fecha o mês corrente com 500; o mês anterior (usado como abertura) não aparece na resposta de
  // propósito, pra exercitar o fallback pro saldo bruto da conta.
  allAccounts: (period) => Promise.resolve(period.month === 3 ? [{ accountId: '1', balance: 500 }] : []),
};

describe('feature:statement — service (fakes)', () => {
  it('load monta o header "Saldo anterior" + as transações com saldo corrente', async () => {
    const txRepo: StatementTxPort = {
      listByAccount: () => Promise.resolve([{ id: '1', date: '2026-03-10', description: 'Compra', amount: -50, type: 'expense' }]),
    };
    const service = createStatementService({ txRepo, balance: fakeBalance, cache: fakeCache([ACCOUNT], { '1': ACCOUNT }) });
    const { rows, raw } = await service.load('1', Period.create(3, 2026), 100);
    expect(rows.length).toBe(2);
    expect(rows[0]?.status).toBe('balance');
    expect(rows[0]?.runningBal).toBe(100);
    expect(rows[1]?.runningBal).toBe(50);
    expect(raw.length).toBe(1);
  });

  it('load funde as compras dos cartões da conta numa linha "Cartões de crédito" no vencimento', async () => {
    const cardAccount = {
      id: '1', name: 'Conta', balance: 0, closingDay: 20, dueDay: 5,
      cards: [{ id: '9', last4: '1234', accountId: '1', active: true }, { id: '10', last4: '5678', accountId: '1', active: true }],
    } as Account;
    const txRepo: StatementTxPort = {
      listByAccount: () =>
        Promise.resolve([
          { id: '1', cardId: '9', date: '2026-03-10', amount: -50, type: 'expense' }, // vence 05/04
          { id: '2', cardId: '10', date: '2026-03-11', amount: -30, type: 'expense' }, // outro cartão, mesma fatura
        ]),
    };
    const service = createStatementService({ txRepo, balance: fakeBalance, cache: fakeCache([cardAccount], { '1': cardAccount }) });
    const { rows } = await service.load('1', Period.create(4, 2026), 100);
    expect(rows.length).toBe(2);
    expect((rows[1] as unknown as { description?: string }).description).toBe('Cartões de crédito');
    expect(rows[1]?.date).toBe('2026-04-05');
    expect(rows[1]?.amount).toBe(-80);
    expect((rows[1] as unknown as { invoice?: boolean }).invoice).toBe(true);
  });

  it('load devolve lista vazia se a conta não existe no cache', async () => {
    const txRepo: StatementTxPort = { listByAccount: () => Promise.resolve([]) };
    const service = createStatementService({ txRepo, balance: fakeBalance, cache: fakeCache([ACCOUNT], { '1': ACCOUNT }) });
    const result = await service.load('999', Period.create(3, 2026), 0);
    expect(result).toEqual({ rows: [], raw: [] });
  });

  it('summary combina o fechamento do período com a abertura (fechamento do mês anterior)', async () => {
    const txRepo: StatementTxPort = { listByAccount: () => Promise.resolve([]) };
    const service = createStatementService({ txRepo, balance: fakeBalance, cache: fakeCache([ACCOUNT], { '1': ACCOUNT }) });
    const out = await service.summary(Period.create(3, 2026));
    expect(out.length).toBe(1);
    expect(out[0]?.closingBalance).toBe(500);
    expect(out[0]?.openingBalance).toBe(999);
  });
});

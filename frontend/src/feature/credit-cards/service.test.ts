import { describe, expect, it } from 'vitest';
import type { Account } from '../../core/kernel/_0_domain/account.ts';
import * as Period from '../../core/kernel/_0_domain/period.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import { createCreditCardService } from './service.ts';
import type { CreditCardsTxRepoPort } from './service.ts';

function fakeCache(): CacheStore {
  return {
    accounts: () =>
      [
        { id: '1', name: 'Com cartão', cards: [{ id: '9', last4: '1234', accountId: '1', active: true }] },
        { id: '2', name: 'Sem cartão', cards: [] },
      ] as Account[],
  } as unknown as CacheStore;
}

function fakeTxRepo(): CreditCardsTxRepoPort {
  return {
    listByAccount: () => Promise.resolve([{ id: '1', cardId: '9', date: '2026-03-10', amount: -100, type: 'expense' }]),
  };
}

describe('feature:credit-cards — service (txRepo/cache fakes)', () => {
  it('listFromCache achata os cartões de todas as contas, carregando a conta dona', () => {
    const service = createCreditCardService({ txRepo: fakeTxRepo(), cache: fakeCache() });
    const out = service.listFromCache();
    expect(out.length).toBe(1);
    expect(out[0]?.last4).toBe('1234');
    expect(out[0]?.account.name).toBe('Com cartão');
  });

  it('accountsWithCards filtra só contas que têm ao menos um cartão', () => {
    const service = createCreditCardService({ txRepo: fakeTxRepo(), cache: fakeCache() });
    const out = service.accountsWithCards();
    expect(out.length).toBe(1);
    expect(out[0]?.name).toBe('Com cartão');
  });

  it('invoiceFor busca a janela do ciclo e filtra pelo cardId', async () => {
    const service = createCreditCardService({ txRepo: fakeTxRepo(), cache: fakeCache() });
    const account = { id: '1', closingDay: 20, dueDay: 5 } as Account;
    const card = { id: '9', last4: '1234' } as Account['cards'][number];
    const period = Period.create(4, 2026);
    const invoice = await service.invoiceFor(card, account, period);
    expect(invoice.dueDate).toBe('2026-04-05');
    expect(invoice.transactions.length).toBe(1);
    expect(invoice.total).toBe(100);
  });
});

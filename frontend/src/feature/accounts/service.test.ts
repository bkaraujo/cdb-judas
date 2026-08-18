import { describe, expect, it } from 'vitest';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import { createAccountService } from './service.ts';
import type { AccountRepository } from './repository.ts';

function fakeRepo(): AccountRepository {
  return {
    list: () => Promise.resolve([{ id: '1', name: 'Conta' }]),
    create: (data) => Promise.resolve({ id: '99', ...data }),
    update: (id, data) => Promise.resolve({ id, ...data }),
    remove: () => Promise.resolve(null),
    listCards: () => Promise.resolve([]),
    createCard: (accountId, data) => Promise.resolve({ id: '5', accountId, ...data }),
    removeCard: () => Promise.resolve(null),
    setCardActive: (accountId, cardId, active) => Promise.resolve({ id: cardId, accountId, last4: '5678', active }),
  };
}

function fakeCache(): CacheStore {
  return {
    accounts: () => [{ id: '1', name: 'Conta (cache)' }],
    findById: ((key: string, id: string) => (key === 'accounts' ? { id, name: 'Achada' } : null)) as CacheStore['findById'],
    subscribe: () => () => {},
  } as unknown as CacheStore;
}

describe('feature:accounts — service (repo/cache fakes)', () => {
  it('list delega pro repo (rede)', async () => {
    const service = createAccountService({ repo: fakeRepo(), cache: fakeCache() });
    const out = await service.list();
    expect(out?.[0]?.name).toBe('Conta');
  });

  it('listCached lê do cache, sem rede', () => {
    const service = createAccountService({ repo: fakeRepo(), cache: fakeCache() });
    expect(service.listCached()[0]?.name).toBe('Conta (cache)');
  });

  it('findById delega pro cache com o tipo "accounts"', () => {
    const service = createAccountService({ repo: fakeRepo(), cache: fakeCache() });
    expect(service.findById('7')?.name).toBe('Achada');
  });

  it('create/update/remove passam pelo repo', async () => {
    const service = createAccountService({ repo: fakeRepo(), cache: fakeCache() });
    const created = await service.create({ name: 'Nova', type: 'CHECKING', color: '#fff' });
    expect(created?.id).toBe('99');
    const updated = await service.update(created?.id as string, { name: 'Renomeada', type: 'CHECKING', color: '#fff' });
    expect(updated?.name).toBe('Renomeada');
    await expect(service.remove(updated?.id as string)).resolves.toBeNull();
  });

  it('addCard/removeCard/setCardActive delegam pro repo', async () => {
    const service = createAccountService({ repo: fakeRepo(), cache: fakeCache() });
    const card = await service.addCard('1', { last4: '5678' });
    expect(card?.last4).toBe('5678');
    const r = await service.setCardActive('1', card?.id as string, false);
    expect(r?.active).toBe(false);
  });

  it('onChange se inscreve no cache (ACCOUNT)', () => {
    const service = createAccountService({ repo: fakeRepo(), cache: fakeCache() });
    const unsubscribe = service.onChange(() => {});
    expect(typeof unsubscribe).toBe('function');
  });
});

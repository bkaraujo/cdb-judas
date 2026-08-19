import { describe, expect, it } from 'vitest';
import type { Account } from '@/core/kernel/_0_domain/account.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { createTransactionService } from '@/feature/transactions/service.ts';
import type { TransactionRepository } from '@/feature/transactions/repository.ts';

function fakeRepo(overrides: Partial<TransactionRepository> = {}): TransactionRepository {
  return {
    list: (params) => Promise.resolve([{ id: '1', description: 'Tx', params } as never]),
    listByAccount: () => Promise.resolve([]),
    create: (data) => Promise.resolve({ id: '99', ...data }),
    update: (id, data) => Promise.resolve({ id, ...data }),
    remove: () => Promise.resolve(null),
    patchStatus: (accountId, id, status) => Promise.resolve({ accountId, id, status } as never),
    transfer: (data) => Promise.resolve({ id: '77', ...data } as never),
    importPreview: () => Promise.resolve(null),
    importConfirm: () => Promise.resolve(null),
    ...overrides,
  };
}

function fakeCache(accounts: Account[] = []): CacheStore {
  return { accounts: () => accounts } as unknown as CacheStore;
}

describe('feature:transactions — service.buildPayload (puro)', () => {
  it('aplica a regra de sinal do domínio (expense negativa, income positiva)', () => {
    const service = createTransactionService({ repo: fakeRepo(), cache: fakeCache() });
    const payload = service.buildPayload({ type: 'expense', amount: 150, description: 'Mercado' });
    expect(payload.amount).toBe(-150);
    expect(payload.type).toBe('expense');
    expect(payload.description).toBe('Mercado');
  });

  it('normaliza o type (ex.: "revenue" vira "income")', () => {
    const service = createTransactionService({ repo: fakeRepo(), cache: fakeCache() });
    const payload = service.buildPayload({ type: 'revenue', amount: 100 });
    expect(payload.type).toBe('income');
    expect(payload.amount).toBe(100);
  });
});

describe('feature:transactions — service (repo/cache fakes)', () => {
  it('list delega pro repo com os params recebidos', async () => {
    const service = createTransactionService({ repo: fakeRepo(), cache: fakeCache() });
    const out = await service.list('status=pending');
    expect((out?.[0] as unknown as { params?: string })?.params).toBe('status=pending');
  });

  it('create/update/patchStatus/remove passam pelo repo', async () => {
    const service = createTransactionService({ repo: fakeRepo(), cache: fakeCache() });
    const created = await service.create({ description: 'Nova' } as never);
    expect(created?.id).toBe('99');
    const patched = await service.patchStatus('1', created?.id as string, 'confirmed', '2026-03-10');
    expect(patched?.status).toBe('confirmed');
    await expect(service.remove('1', patched?.id as string)).resolves.toBeNull();
  });

  it('listForPeriod colapsa via Domain.Invoice usando as contas do cache', async () => {
    // Sem contas com cartão no cache: fetchWindow devolve a janela do próprio mês, sem alargar —
    // collapse não tem nada pra colapsar, só repassa o que o repo devolveu.
    const service = createTransactionService({ repo: fakeRepo(), cache: fakeCache() });
    const out = await service.listForPeriod(Period.create(3, 2026));
    expect(Array.isArray(out.rows)).toBe(true);
    expect(Array.isArray(out.raw)).toBe(true);
  });

  it('listForPeriod entrega uma linha "Cartões <conta>" por conta, com as compras cruas em raw', async () => {
    const account = {
      id: '1', name: 'Nubank', closingDay: 20, dueDay: 5,
      cards: [{ id: '9', last4: '1234', accountId: '1', active: true }, { id: '10', last4: '5678', accountId: '1', active: true }],
    } as Account;
    const repo = fakeRepo({
      list: () =>
        Promise.resolve([
          { id: '1', cardId: '9', accountId: '1', date: '2026-03-10', amount: -50, type: 'expense' } as never,
          { id: '2', cardId: '10', accountId: '1', date: '2026-03-11', amount: -30, type: 'expense' } as never,
        ]),
    });
    const service = createTransactionService({ repo, cache: fakeCache([account]) });
    const out = await service.listForPeriod(Period.create(4, 2026));
    expect(out.rows.length).toBe(1);
    expect((out.rows[0] as { description?: string }).description).toBe('Cartões Nubank');
    expect((out.rows[0] as { amount?: number }).amount).toBe(-80);
    expect(out.raw.length).toBe(2);
  });
});

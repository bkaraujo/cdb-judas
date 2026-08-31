import {describe, expect, it} from 'vitest';
import type {PayableTxRepoPort} from '@/feature/accounts-payable/service.ts';
import {createPayableService} from '@/feature/accounts-payable/service.ts';

describe('feature:accounts-payable — service (repo fake)', () => {
  const fakeRepo: PayableTxRepoPort = {
    list: () => Promise.resolve([{ id: '1', description: 'Aluguel', date: '2026-03-05', amount: -1200, accountId: '7', categoryId: '3', status: 'pending' }]),
  };

  it('listPayable adapta transações pro formato da tela (Domain.Payable)', async () => {
    const service = createPayableService({ repo: fakeRepo });
    const out = await service.listPayable();
    expect(out.length).toBe(1);
    expect(out[0]?.due).toBe('2026-03-05');
    expect(out[0]?.amount).toBe(1200);
    expect(out[0]?.type).toBe('PAYABLE');
  });

  it('listReceivable usa o mesmo adapter com type RECEIVABLE', async () => {
    const service = createPayableService({ repo: fakeRepo });
    const out = await service.listReceivable();
    expect(out[0]?.type).toBe('RECEIVABLE');
  });
});

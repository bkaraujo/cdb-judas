import { describe, expect, it } from 'vitest';
import type { TransactionResponse } from '../../api/overrides.ts';
import { createDashboardService } from './service.ts';
import type { DashboardRepository } from './repository.ts';

describe('feature:dashboard — service (repo fake)', () => {
  const fakeRepo: DashboardRepository = {
    getMonthlyResult: (month, year) => Promise.resolve({ month, year, receitas: 0, despesas: 0 }),
    // id carrega o `limit` recebido — único jeito de provar o repasse do parâmetro sem inventar
    // um campo fora do wire type real.
    getRecentTransactions: (limit) => Promise.resolve([{ id: String(limit) } as unknown as TransactionResponse]),
  };

  it('monthlyResult/recentTransactions delegam pro repo', async () => {
    const service = createDashboardService({ repo: fakeRepo });
    const r = await service.monthlyResult({ month: 3, year: 2026 });
    expect(r?.month).toBe(3);
    const tx = await service.recentTransactions(7);
    expect(tx?.[0]?.id).toBe('7');
  });
});

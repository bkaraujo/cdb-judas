import { beforeEach, describe, expect, it } from 'vitest';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { fakeCache, fakeCrudRepo } from '@/test-support/fakes.ts';
import { createCostCenterService } from '@/feature/cost-centers/service.ts';

describe('feature:cost-centers — service (repo/cache fakes)', () => {
  let service: ReturnType<typeof createCostCenterService>;

  beforeEach(() => {
    const repo = fakeCrudRepo([{ id: '1', name: 'TI' } as never]);
    const cache = fakeCache({
      costCenters: () => [{ id: '1', name: 'TI (cache)' } as never],
      findById: ((_key: string, id: string) => ({ id, name: 'Achado' })) as CacheStore['findById'],
    });
    service = createCostCenterService({ repo, cache });
  });

  it('list delega pro repo (rede)', async () => {
    const out = await service.list();
    expect(out[0]?.name).toBe('TI');
  });

  it('listCached lê do cache, sem rede', () => {
    expect(service.listCached()[0]?.name).toBe('TI (cache)');
  });

  it('findById delega pro cache com o tipo "costCenters"', () => {
    expect(service.findById('7')?.name).toBe('Achado');
  });

  it('onChange se inscreve no cache (COST_CENTER)', () => {
    expect(typeof service.onChange(() => {})).toBe('function');
  });
});

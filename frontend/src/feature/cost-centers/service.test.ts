import { describe, expect, it } from 'vitest';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import { createCostCenterService } from './service.ts';
import type { CostCenterRepository } from './repository.ts';

function fakeRepo(): CostCenterRepository {
  return { list: () => Promise.resolve([{ id: '1', name: 'TI' } as never]) };
}

function fakeCache(): CacheStore {
  return {
    costCenters: () => [{ id: '1', name: 'TI (cache)' } as never],
    findById: ((_key: string, id: string) => ({ id, name: 'Achado' })) as CacheStore['findById'],
    subscribe: () => () => {},
  } as unknown as CacheStore;
}

describe('feature:cost-centers — service (repo/cache fakes)', () => {
  it('list delega pro repo (rede)', async () => {
    const service = createCostCenterService({ repo: fakeRepo(), cache: fakeCache() });
    const out = await service.list();
    expect(out[0]?.name).toBe('TI');
  });

  it('listCached lê do cache, sem rede', () => {
    const service = createCostCenterService({ repo: fakeRepo(), cache: fakeCache() });
    expect(service.listCached()[0]?.name).toBe('TI (cache)');
  });

  it('findById delega pro cache com o tipo "costCenters"', () => {
    const service = createCostCenterService({ repo: fakeRepo(), cache: fakeCache() });
    expect(service.findById('7')?.name).toBe('Achado');
  });

  it('onChange se inscreve no cache (COST_CENTER)', () => {
    const service = createCostCenterService({ repo: fakeRepo(), cache: fakeCache() });
    expect(typeof service.onChange(() => {})).toBe('function');
  });
});

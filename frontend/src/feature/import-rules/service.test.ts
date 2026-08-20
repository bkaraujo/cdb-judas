import { describe, expect, it } from 'vitest';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { createImportRuleService } from '@/feature/import-rules/service.ts';
import type { ImportRuleRepository } from '@/feature/import-rules/repository.ts';

function fakeRepo(): ImportRuleRepository {
  return {
    list: () => Promise.resolve([{ id: '1', name: 'Padaria', triggers: ['Padaria'] }]),
    create: (data) => Promise.resolve({ id: '99', ...data }),
    update: (id, data) => Promise.resolve({ id, ...data }),
    remove: () => Promise.resolve(null),
  };
}

function fakeCache(): CacheStore {
  return {
    importRules: () => [{ id: '1', name: 'Padaria (cache)', triggers: ['Padaria'] }],
    findById: ((key: string, id: string) => (key === 'importRules' ? { id, name: 'Achada', triggers: ['Achada'] } : null)) as CacheStore['findById'],
    subscribe: () => () => {},
  } as unknown as CacheStore;
}

describe('feature:import-rules — service (repo/cache fakes)', () => {
  it('list delega pro repo (rede)', async () => {
    const service = createImportRuleService({ repo: fakeRepo(), cache: fakeCache() });
    const out = await service.list();
    expect(out?.[0]?.name).toBe('Padaria');
    expect(out?.[0]?.triggers).toEqual(['Padaria']);
  });

  it('listCached lê do cache, sem rede', () => {
    const service = createImportRuleService({ repo: fakeRepo(), cache: fakeCache() });
    expect(service.listCached()[0]?.name).toBe('Padaria (cache)');
    expect(service.listCached()[0]?.triggers).toEqual(['Padaria']);
  });

  it('findById delega pro cache com o tipo "importRules"', () => {
    const service = createImportRuleService({ repo: fakeRepo(), cache: fakeCache() });
    expect(service.findById('7')?.name).toBe('Achada');
    expect(service.findById('7')?.triggers).toEqual(['Achada']);
  });

  it('create/update/remove passam pelo repo', async () => {
    const service = createImportRuleService({ repo: fakeRepo(), cache: fakeCache() });
    const created = await service.create({ name: 'Nova', triggers: ['Nova'] });
    expect(created?.id).toBe('99');
    const updated = await service.update(created?.id as string, { name: 'Renomeada', triggers: ['Renomeada'] });
    expect(updated?.name).toBe('Renomeada');
    expect(updated?.triggers).toEqual(['Renomeada']);
    await expect(service.remove(updated?.id as string)).resolves.toBeNull();
  });

  it('onChange se inscreve no cache (IMPORT_RULE)', () => {
    const service = createImportRuleService({ repo: fakeRepo(), cache: fakeCache() });
    expect(typeof service.onChange(() => {})).toBe('function');
  });
});

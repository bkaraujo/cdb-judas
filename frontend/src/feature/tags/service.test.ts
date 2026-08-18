import { describe, expect, it } from 'vitest';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import { createTagService } from './service.ts';
import type { TagRepository } from './repository.ts';

function fakeRepo(): TagRepository {
  return {
    list: () => Promise.resolve([{ id: '1', name: 'Viagem' }]),
    create: (data) => Promise.resolve({ id: '99', ...data }),
    update: (id, data) => Promise.resolve({ id, ...data }),
    // DELETE real é 204 (sem corpo) — o repo tipa remove como Promise<void|null>.
    remove: () => Promise.resolve(null),
  };
}

function fakeCache(): CacheStore {
  return {
    tags: () => [{ id: '1', name: 'Viagem (cache)', color: null }],
    findById: ((_key: string, id: string) => ({ id, name: 'Achada', color: null })) as CacheStore['findById'],
    subscribe: () => () => {},
  } as unknown as CacheStore;
}

describe('feature:tags — service (repo/cache fakes)', () => {
  it('list delega pro repo (rede)', async () => {
    const service = createTagService({ repo: fakeRepo(), cache: fakeCache() });
    const out = await service.list();
    expect(out?.[0]?.name).toBe('Viagem');
  });

  it('listCached lê do cache, sem rede', () => {
    const service = createTagService({ repo: fakeRepo(), cache: fakeCache() });
    expect(service.listCached()[0]?.name).toBe('Viagem (cache)');
  });

  it('findById delega pro cache com o tipo "tags"', () => {
    const service = createTagService({ repo: fakeRepo(), cache: fakeCache() });
    expect(service.findById('7')?.name).toBe('Achada');
  });

  it('create/update/remove passam pelo repo', async () => {
    const service = createTagService({ repo: fakeRepo(), cache: fakeCache() });
    const created = await service.create({ name: 'Nova', color: '#fff' });
    expect(created?.id).toBe('99');
    const updated = await service.update(created?.id as string, { name: 'Renomeada', color: '#fff' });
    expect(updated?.name).toBe('Renomeada');
    await expect(service.remove(updated?.id as string)).resolves.toBeNull();
  });

  it('onChange se inscreve no cache (TAG)', () => {
    const service = createTagService({ repo: fakeRepo(), cache: fakeCache() });
    expect(typeof service.onChange(() => {})).toBe('function');
  });
});

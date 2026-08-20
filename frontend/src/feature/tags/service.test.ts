import { beforeEach, describe, expect, it } from 'vitest';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { fakeCache, fakeCrudRepo } from '@/test-support/fakes.ts';
import { createTagService } from '@/feature/tags/service.ts';

describe('feature:tags — service (repo/cache fakes)', () => {
  let service: ReturnType<typeof createTagService>;

  beforeEach(() => {
    const repo = fakeCrudRepo([{ id: '1', name: 'Viagem' }]);
    const cache = fakeCache({
      tags: () => [{ id: '1', name: 'Viagem (cache)', color: null }],
      findById: ((_key: string, id: string) => ({ id, name: 'Achada', color: null })) as CacheStore['findById'],
    });
    service = createTagService({ repo, cache });
  });

  it('list delega pro repo (rede)', async () => {
    const out = await service.list();
    expect(out?.[0]?.name).toBe('Viagem');
  });

  it('listCached lê do cache, sem rede', () => {
    expect(service.listCached()[0]?.name).toBe('Viagem (cache)');
  });

  it('findById delega pro cache com o tipo "tags"', () => {
    expect(service.findById('7')?.name).toBe('Achada');
  });

  it('create/update/remove passam pelo repo', async () => {
    const created = await service.create({ name: 'Nova', color: '#fff' });
    expect(created?.id).toBe('99');
    const updated = await service.update(created?.id as string, { name: 'Renomeada', color: '#fff' });
    expect(updated?.name).toBe('Renomeada');
    await expect(service.remove(updated?.id as string)).resolves.toBeNull();
  });

  it('onChange se inscreve no cache (TAG)', () => {
    expect(typeof service.onChange(() => {})).toBe('function');
  });
});

import { beforeEach, describe, expect, it } from 'vitest';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import * as CategoryDomain from '@/core/kernel/_0_domain/category.ts';
import type { Category } from '@/core/kernel/_0_domain/category.ts';
import { fakeCache, fakeCrudRepo } from '@/test-support/fakes.ts';
import { createCategoryService } from '@/feature/categories/service.ts';

const CATS: Category[] = [
  { id: '1', name: 'Moradia', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
  { id: '2', name: 'Aluguel', nature: 'EXPENSE', parentId: '1', isSystem: false, active: true },
];

describe('feature:categories — service (repo/cache fakes)', () => {
  let service: ReturnType<typeof createCategoryService>;

  beforeEach(() => {
    const repo = fakeCrudRepo(CATS as never[]);
    const cache = fakeCache({
      categories: () => CATS,
      findById: ((key: string, id: string) => (key === 'categories' ? CategoryDomain.byId(CATS, id) : null)) as CacheStore['findById'],
    });
    service = createCategoryService({ repo, cache });
  });

  it('listCached lê do cache', () => {
    expect(service.listCached().length).toBe(2);
  });

  it('findById delega pro cache com o tipo "categories"', () => {
    expect(service.findById('2')?.name).toBe('Aluguel');
  });

  it('rootsByNature/childrenOf/labelChain/eligibleParents/isEffectivelyActive delegam pro Domain.Category com o cache', () => {
    expect(service.rootsByNature('EXPENSE').length).toBe(1);
    expect(service.childrenOf('1').length).toBe(1);
    expect(service.labelChain('2')).toBe('Moradia · Aluguel');
    expect(service.eligibleParents('EXPENSE', '1').length).toBe(0);
    expect(service.isEffectivelyActive('2')).toBe(true);
  });

  it('create/update/remove passam pelo repo', async () => {
    const created = await service.create({ name: 'Nova', nature: 'EXPENSE' });
    expect(created?.id).toBe('99');
    const updated = await service.update(created?.id as string, { name: 'Renomeada' });
    expect(updated?.name).toBe('Renomeada');
    await expect(service.remove(updated?.id as string)).resolves.toBeNull();
  });

  it('onChange se inscreve no cache (CATEGORY)', () => {
    const unsubscribe = service.onChange(() => {});
    expect(typeof unsubscribe).toBe('function');
  });
});

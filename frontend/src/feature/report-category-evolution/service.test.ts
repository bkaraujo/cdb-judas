import { describe, expect, it } from 'vitest';
import type { Account } from '@/core/kernel/_0_domain/account.ts';
import type { Category } from '@/core/kernel/_0_domain/category.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { createCategoryEvolutionService } from '@/feature/report-category-evolution/service.ts';
import type { EvolutionTx } from '@/feature/report-category-evolution/domain.ts';

describe('feature:report-category-evolution — service', () => {
  it('load busca com janela alargada para período mensal', async () => {
    const capturedQueries: string[] = [];
    const txRepo = {
      list: async (query: string) => {
        capturedQueries.push(query);
        return [
          { date: '2026-03-15', amount: 100, type: 'expense', status: 'confirmed', categoryId: 'cat1' },
        ] as EvolutionTx[];
      },
    };

    const mockCache = {
      accounts: () => [] as Account[],
      categories: () => [
        { id: 'cat1', name: 'Cat 1', nature: 'EXPENSE', parentId: null, isSystem: false, active: true } as Category,
      ],
    } as CacheStore;

    const service = createCategoryEvolutionService({ txRepo, cache: mockCache });
    const matrix = await service.load('month', Period.create(3, 2026), 3);

    expect(capturedQueries).toHaveLength(1);
    const query = capturedQueries[0];
    expect(query).toMatch(/^dateFrom=/);
    expect(query).toMatch(/&dateTo=/);
    expect(query).not.toContain('limit'); // Sem limit
    expect(matrix.buckets).toHaveLength(3);
  });

  it('load busca com janela alargada para período anual', async () => {
    const capturedQueries: string[] = [];
    const txRepo = {
      list: async (query: string) => {
        capturedQueries.push(query);
        return [] as EvolutionTx[];
      },
    };

    const mockCache = {
      accounts: () => [] as Account[],
      categories: () => [] as Category[],
    } as CacheStore;

    const service = createCategoryEvolutionService({ txRepo, cache: mockCache });
    await service.load('year', Period.create(6, 2026), 3);

    expect(capturedQueries).toHaveLength(1);
    const query = capturedQueries[0];
    expect(query).toMatch(/^dateFrom=/);
    expect(query).toMatch(/&dateTo=/);
    expect(query).not.toContain('limit');
  });

  it('load retorna matriz com nº correto de buckets', async () => {
    const txRepo = {
      list: async () => [] as EvolutionTx[],
    };

    const mockCache = {
      accounts: () => [] as Account[],
      categories: () => [] as Category[],
    } as CacheStore;

    const service = createCategoryEvolutionService({ txRepo, cache: mockCache });
    const matrix = await service.load('month', Period.create(3, 2026), 12);

    expect(matrix.buckets).toHaveLength(12);
  });

  it('load delega agregação ao domain', async () => {
    const txs: EvolutionTx[] = [
      { date: '2026-03-15', amount: 100, type: 'expense', status: 'confirmed', categoryId: 'cat1' },
      { date: '2026-03-20', amount: 50, type: 'expense', status: 'confirmed', categoryId: 'cat1' },
    ];

    const txRepo = {
      list: async () => txs,
    };

    const categories = [
      { id: 'cat1', name: 'Cat 1', nature: 'EXPENSE', parentId: null, isSystem: false, active: true } as Category,
    ];

    const mockCache = {
      accounts: () => [] as Account[],
      categories: () => categories,
    } as CacheStore;

    const service = createCategoryEvolutionService({ txRepo, cache: mockCache });
    const matrix = await service.load('month', Period.create(3, 2026), 1);

    const catRow = matrix.rows.find((r) => r.id === 'cat1');
    expect(catRow?.total).toBe(-150); // Agregação funcionou
  });
});

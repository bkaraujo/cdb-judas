import {describe, expect, it} from 'vitest';
import {createBudgetService} from '@/feature/budget/service.ts';
import type {BudgetRepository} from '@/feature/budget/repository.ts';

describe('feature:budget — service (repo fake)', () => {
  const fakeRepo: BudgetRepository = {
    list: (m, y) => Promise.resolve([{ id: '1', categoryId: '2', name: '', budgeted: 100, spent: 50, month: m, year: y, color: null, icon: null }]),
    create: (data) => Promise.resolve({ id: '99', categoryId: data.categoryId, name: '', budgeted: data.budgeted, spent: 0, month: data.month, year: data.year, color: data.color ?? null, icon: data.icon ?? null }),
    update: (id, data) => Promise.resolve({ id, categoryId: '2', name: '', budgeted: data.budgeted, spent: 0, month: 3, year: 2026, color: data.color ?? null, icon: data.icon ?? null }),
    remove: () => Promise.resolve(),
  };

  it('loadPeriod delega pro repo.list com month/year do período', async () => {
    const service = createBudgetService({ repo: fakeRepo });
    const items = await service.loadPeriod({ month: 3, year: 2026 });
    expect(items?.[0]?.month).toBe(3);
    expect(items?.[0]?.year).toBe(2026);
  });

  it('summary agrega total e overspending via Domain.Budget', () => {
    const service = createBudgetService({ repo: fakeRepo });
    const summary = service.summary([
      { id: '1', categoryId: '2', name: '', budgeted: 200, spent: 250, month: 3, year: 2026, color: null, icon: null },
      { id: '2', categoryId: '3', name: '', budgeted: 200, spent: 50, month: 3, year: 2026, color: null, icon: null },
    ]);
    expect(summary).toEqual({ total: 2, overspending: 1 });
  });

  it('save/create/remove passam pelo repo injetado', async () => {
    const service = createBudgetService({ repo: fakeRepo });
    const created = await service.create({ categoryId: '2', month: 3, year: 2026, budgeted: 300 });
    expect(created?.id).toBe('99');
    const saved = await service.save(created?.id as string, { budgeted: 350 });
    expect(saved?.budgeted).toBe(350);
    await expect(service.remove(saved?.id as string)).resolves.toBeUndefined();
  });
});

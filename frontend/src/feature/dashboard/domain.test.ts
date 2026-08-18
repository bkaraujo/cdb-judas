import { describe, expect, it } from 'vitest';
import { isActive } from '../accounts-payable/domain.ts';
import type { Account } from '../../core/kernel/_0_domain/account.ts';
import type { Category } from '../../core/kernel/_0_domain/category.ts';
import * as DashboardAggregations from './domain.ts';
import * as BalanceSheet from './domain.ts';

describe('feature:dashboard — Domain.BalanceSheet', () => {
  it('compute soma o saldo corrente de todas as contas (sem passivo no modelo atual)', () => {
    const sheet = BalanceSheet.compute([{ currentBalance: 100 }, { currentBalance: -30 }] as Account[]);
    expect(sheet).toEqual({ assets: 70, liabilities: 0, equity: 70 });
  });

  it('lista vazia devolve tudo zerado', () => {
    expect(BalanceSheet.compute([])).toEqual({ assets: 0, liabilities: 0, equity: 0 });
  });
});

describe('feature:dashboard — Domain.DashboardAggregations', () => {
  const CATS = [
    { id: '1', name: 'Mercado', nature: 'EXPENSE' },
    { id: '2', name: 'Salário', nature: 'INCOME' },
  ] as Category[];

  it('txIsExpense usa o type quando presente, senão a natureza da categoria, senão o sinal', () => {
    expect(DashboardAggregations.txIsExpense({ type: 'expense' })).toBe(true);
    expect(DashboardAggregations.txIsExpense({ type: 'income' })).toBe(false);
    expect(DashboardAggregations.txIsExpense({}, 'EXPENSE')).toBe(true);
    expect(DashboardAggregations.txIsExpense({ amount: -50 })).toBe(true);
  });

  it('categoryNameFor/categoryNatureFor resolvem contra a lista de categorias', () => {
    expect(DashboardAggregations.categoryNameFor({ categoryId: '1' }, CATS)).toBe('Mercado');
    expect(DashboardAggregations.categoryNameFor({ categoryId: '999' }, CATS)).toBe('Sem categoria');
    expect(DashboardAggregations.categoryNatureFor({ categoryId: '1' }, CATS)).toBe('EXPENSE');
  });

  it('expenseByCategory soma |amount| por categoria só das despesas, ordenado desc', () => {
    const out = DashboardAggregations.expenseByCategory(
      [
        { type: 'expense', categoryId: '1', amount: -100 },
        { type: 'expense', categoryId: '1', amount: -50 },
        { type: 'income', categoryId: '2', amount: 500 },
      ],
      CATS,
    );
    expect(out).toEqual([{ name: 'Mercado', value: 150 }]);
  });

  it('topN corta a lista', () => {
    expect(DashboardAggregations.topN([1, 2, 3, 4, 5, 6], 3)).toEqual([1, 2, 3]);
    expect(DashboardAggregations.topN([1, 2], 5).length).toBe(2);
  });

  it('monthlySeries agrupa receitas/despesas por mês, com "now" injetado — determinístico', () => {
    const now = new Date(2026, 7, 11); // 11/08/2026 local — injetado, sem Date.now()
    const series = DashboardAggregations.monthlySeries(
      [
        { date: '2026-06-15', type: 'expense', amount: -100 },
        { date: '2026-08-15', type: 'income', amount: 500 },
      ],
      3,
      now,
    );

    expect(series.length).toBe(3);
    expect(series.map((b) => b.month)).toEqual([6, 7, 8]);
    expect(series[0]?.despesas).toBe(100);
    expect(series[2]?.receitas).toBe(500);
  });

  it('upcomingPayables filtra ativos (via isActive injetado) e ordena por vencimento', () => {
    const out = DashboardAggregations.upcomingPayables(
      [
        { id: 1, status: 'pending', due: '2026-03-20' },
        { id: 2, status: 'cancelled', due: '2026-03-10' },
        { id: 3, status: 'confirmed', due: '2026-03-05' },
      ],
      isActive,
    );
    expect(out.map((p) => p.id)).toEqual([3, 1]);
  });
});

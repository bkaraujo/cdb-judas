import { describe, expect, it } from 'vitest';
import * as BudgetDomain from '@/feature/budget/domain.ts';

describe('feature:budget — domain', () => {
  it('consumptionPct clampa em [0,100] e não divide por zero', () => {
    expect(BudgetDomain.consumptionPct(50, 200)).toBe(25);
    expect(BudgetDomain.consumptionPct(250, 200)).toBe(100);
    expect(BudgetDomain.consumptionPct(50, 0)).toBe(0);
  });

  it('isOverBudget só é true com budgeted positivo e spent maior', () => {
    expect(BudgetDomain.isOverBudget(250, 200)).toBe(true);
    expect(BudgetDomain.isOverBudget(150, 200)).toBe(false);
    expect(BudgetDomain.isOverBudget(250, 0)).toBe(false);
  });

  it('overspendCount soma só os itens estourados', () => {
    const items = [
      { spent: 250, budgeted: 200 },
      { spent: 100, budgeted: 200 },
      { spent: 300, budgeted: 100 },
    ];
    expect(BudgetDomain.overspendCount(items)).toBe(2);
    expect(BudgetDomain.overspendCount([])).toBe(0);
  });

  it('barColor mapeia faixas de consumo pros tokens de cor', () => {
    expect(BudgetDomain.barColor(90)).toBe('expense');
    expect(BudgetDomain.barColor(70)).toBe('warning');
    expect(BudgetDomain.barColor(30)).toBe('accent');
  });

  it('remainingOrOver retorna o restante ou o valor estourado', () => {
    expect(BudgetDomain.remainingOrOver(50, 200)).toEqual({ value: 150, over: false });
    expect(BudgetDomain.remainingOrOver(250, 200)).toEqual({ value: 50, over: true });
  });
});

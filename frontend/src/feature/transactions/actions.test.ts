import { describe, expect, it } from 'vitest';
import type { Category } from '../../core/kernel/_0_domain/category.ts';
import { isTransfer } from './actions.ts';

const CATEGORIES: Category[] = [
  { id: '1', name: 'Mercado', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
  { id: '9', name: 'Transferência', nature: 'EXPENSE', parentId: null, isSystem: true, active: true },
];

describe('feature:transactions — actions.isTransfer', () => {
  it('perna com categoria de sistema (isSystem) é transferência', () => {
    expect(isTransfer({ categoryId: '9' }, CATEGORIES)).toBe(true);
  });

  it('parcela com groupId e categoria comum não é transferência', () => {
    expect(isTransfer({ groupId: 'g1', categoryId: '1' }, CATEGORIES)).toBe(false);
  });

  it('sem transação ou sem categoria não é transferência', () => {
    expect(isTransfer(null, CATEGORIES)).toBe(false);
    expect(isTransfer({ categoryId: null }, CATEGORIES)).toBe(false);
  });
});

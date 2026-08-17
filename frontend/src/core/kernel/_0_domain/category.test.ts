import { describe, expect, it } from 'vitest';
import * as Category from './category.ts';

const CATS: Category.Category[] = [
  { id: '1', name: 'Moradia', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
  { id: '2', name: 'Aluguel', nature: 'EXPENSE', parentId: '1', isSystem: false, active: true },
  { id: '3', name: 'Condomínio', nature: 'EXPENSE', parentId: '1', isSystem: false, active: false },
  { id: '4', name: 'Salário', nature: 'INCOME', parentId: null, isSystem: false, active: true },
];

describe('kernel:category', () => {
  it('normalize aplica defaults e lê parentId direto do wire', () => {
    // CategoryResponse não tem `parent` (objeto aninhado) nem `description` — só `parentId`/`name`
    // diretos. O fallback pra esses dois é remoção pré-aprovada (.claude/plan.md, Fase 3).
    const c = Category.normalize({ id: '1', name: 'X', parentId: '9' });
    expect(c?.parentId).toBe('9');
    expect(c?.nature).toBe('EXPENSE');
    expect(c?.active).toBe(true);
  });

  it('isRoot é true só sem parentId', () => {
    expect(Category.isRoot(CATS[0])).toBe(true);
    expect(Category.isRoot(CATS[1])).toBe(false);
  });

  it('byId busca por id (comparação por string)', () => {
    expect(Category.byId(CATS, '2')?.name).toBe('Aluguel');
    expect(Category.byId(CATS, '2')?.name).toBe('Aluguel');
    expect(Category.byId(CATS, '999')).toBe(null);
  });

  it('childrenOf lista só os filhos diretos', () => {
    const kids = Category.childrenOf(CATS, '1');
    expect(kids.length).toBe(2);
    expect(kids.map((c) => c.id)).toEqual(['2', '3']);
  });

  it('labelChain monta "pai · filho" só pra subcategoria', () => {
    expect(Category.labelChain(CATS, '2')).toBe('Moradia · Aluguel');
    expect(Category.labelChain(CATS, '1')).toBe('Moradia');
    expect(Category.labelChain(CATS, '999')).toBe('');
  });

  it('rootsByNature filtra raízes pela natureza', () => {
    const roots = Category.rootsByNature(CATS, 'EXPENSE');
    expect(roots.length).toBe(1);
    expect(roots[0]?.id).toBe('1');
  });

  it('isEffectivelyActive propaga inatividade do ancestral pro filho', () => {
    expect(Category.isEffectivelyActive(CATS, '2')).toBe(true);
    expect(Category.isEffectivelyActive(CATS, '3')).toBe(false);
  });

  it('eligibleParents exclui o próprio id', () => {
    const eligible = Category.eligibleParents(CATS, 'EXPENSE', '1');
    expect(eligible.length).toBe(0);
  });
});

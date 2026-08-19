import { describe, expect, it } from 'vitest';
import * as ImportRuleMatcher from '@/feature/import-rules/domain.ts';

describe('feature:import-rules — domain (ImportRuleMatcher)', () => {
  it('normalize maiusculiza e remove acentos', () => {
    expect(ImportRuleMatcher.normalize('Água Mineral')).toBe('AGUA MINERAL');
    expect(ImportRuleMatcher.normalize('  padaria  ')).toBe('PADARIA');
    expect(ImportRuleMatcher.normalize(null)).toBe('');
  });

  it('match acha a regra cujo nome é substring da descrição, ignorando acento/caixa', () => {
    const rules = [{ name: 'Padaria' }, { name: 'Mercado' }];
    expect(ImportRuleMatcher.match('COMPRA PADARIA DO ZE', rules)?.name).toBe('Padaria');
    expect(ImportRuleMatcher.match('compra agua mineral', [{ name: 'Água' }])?.name).toBe('Água');
    expect(ImportRuleMatcher.match('Nada a ver', rules)).toBe(null);
  });

  it('match resolve empate pela ordem do array (primeira regra que casa vence)', () => {
    const rules = [{ name: 'Padaria' }, { name: 'Padaria do Zé' }];
    expect(ImportRuleMatcher.match('COMPRA PADARIA DO ZE', rules)?.name).toBe('Padaria');
  });

  it('match com entrada vazia/sem regras devolve null sem lançar', () => {
    expect(ImportRuleMatcher.match('', [{ name: 'X' }])).toBe(null);
    expect(ImportRuleMatcher.match('Compra', [])).toBe(null);
    expect(ImportRuleMatcher.match('Compra', null)).toBe(null);
  });
});

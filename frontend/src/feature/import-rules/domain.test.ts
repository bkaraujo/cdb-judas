import {describe, expect, it} from 'vitest';
import * as ImportRuleMatcher from '@/feature/import-rules/domain.ts';

describe('feature:import-rules — domain (ImportRuleMatcher)', () => {
  it('normalize maiusculiza e remove acentos', () => {
    expect(ImportRuleMatcher.normalize('Água Mineral')).toBe('AGUA MINERAL');
    expect(ImportRuleMatcher.normalize('  padaria  ')).toBe('PADARIA');
    expect(ImportRuleMatcher.normalize(null)).toBe('');
  });

  it('match acha a regra cujo gatilho é substring da descrição, ignorando acento/caixa', () => {
    const rules = [
      { name: 'Padaria (rótulo)', triggers: ['Padaria'] },
      { name: 'Mercado (rótulo)', triggers: ['Mercado'] }
    ];
    expect(ImportRuleMatcher.match('COMPRA PADARIA DO ZE', rules)?.name).toBe('Padaria (rótulo)');
    expect(ImportRuleMatcher.match('compra agua mineral', [{ name: 'Agua (rótulo)', triggers: ['Água'] }])?.name).toBe('Agua (rótulo)');
    expect(ImportRuleMatcher.match('Nada a ver', rules)).toBe(null);
  });

  it('match com múltiplos gatilhos casa se qualquer um for substring da descrição', () => {
    const rule = { name: 'Padaria (rótulo)', triggers: ['Padaria', 'PDR SAO JOSE'] };
    expect(ImportRuleMatcher.match('COMPRA PDR SAO JOSE', [rule])?.name).toBe('Padaria (rótulo)');
    expect(ImportRuleMatcher.match('compra padaria', [rule])?.name).toBe('Padaria (rótulo)');
  });

  it('match resolve empate pela ordem do array (primeira regra que casa vence)', () => {
    const rules = [
      { name: 'Padaria (rótulo)', triggers: ['Padaria'] },
      { name: 'Padaria do Zé (rótulo)', triggers: ['Padaria do Zé'] }
    ];
    expect(ImportRuleMatcher.match('COMPRA PADARIA DO ZE', rules)?.name).toBe('Padaria (rótulo)');
  });

  it('match com entrada vazia/sem regras devolve null sem lançar', () => {
    expect(ImportRuleMatcher.match('', [{ name: 'X', triggers: ['X'] }])).toBe(null);
    expect(ImportRuleMatcher.match('Compra', [])).toBe(null);
    expect(ImportRuleMatcher.match('Compra', null)).toBe(null);
  });

  it('match ignora regras sem triggers', () => {
    const rules = [
      { name: 'No triggers', triggers: [] },
      { name: 'Padaria (rótulo)', triggers: ['Padaria'] }
    ];
    expect(ImportRuleMatcher.match('COMPRA PADARIA', rules)?.name).toBe('Padaria (rótulo)');
  });
});

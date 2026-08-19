import { describe, expect, it } from 'vitest';
import * as Transaction from './transaction.ts';

describe('kernel:transaction', () => {
  it('normalizeType mapeia aliases e cai pra expense por padrão', () => {
    expect(Transaction.normalizeType('income')).toBe('income');
    expect(Transaction.normalizeType('revenue')).toBe('income');
    expect(Transaction.normalizeType('transfer')).toBe('transfer');
    expect(Transaction.normalizeType('bogus')).toBe('expense');
  });

  it('signedAmount: negativo pra expense, positivo pros demais', () => {
    expect(Transaction.signedAmount('expense', 100)).toBe(-100);
    expect(Transaction.signedAmount('income', 100)).toBe(100);
    expect(Transaction.signedAmount('transfer', -50)).toBe(50);
  });

  it('describe só acrescenta sufixo de parcela quando totalInstallments > 1', () => {
    expect(Transaction.describe({ description: 'Amazon', totalInstallments: 3, installmentNumber: 2 })).toBe('Amazon (2 de 3)');
    expect(Transaction.describe({ description: 'Café', totalInstallments: 1, installmentNumber: 1 })).toBe('Café');
    expect(Transaction.describe(null)).toBe('');
  });


  it('isValidTransfer exige conta origem diferente da destino', () => {
    expect(Transaction.isValidTransfer('a', 'b')).toBe(true);
    expect(Transaction.isValidTransfer('a', 'a')).toBe(false);
    expect(Transaction.isValidTransfer(null, 'b')).toBe(false);
  });

  it('isToday compara contra data de referência injetada — determinístico', () => {
    // isToday faz `new Date(t.date)` internamente — string ISO sem hora vira meia-noite UTC. ref
    // precisa ser construído do mesmo jeito (não new Date(y,m,d), que é meia-noite local) pra não
    // divergir em fusos com offset negativo (ex. America/Sao_Paulo).
    const ref = new Date('2026-08-11');
    expect(Transaction.isToday({ date: '2026-08-11' }, ref)).toBe(true);
    expect(Transaction.isToday({ date: '2026-08-10' }, ref)).toBe(false);
  });
});

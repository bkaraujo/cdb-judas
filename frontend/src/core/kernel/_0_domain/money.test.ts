import { describe, expect, it } from 'vitest';
import * as Money from './money.ts';

describe('kernel:money', () => {
  it('format sempre devolve valor absoluto em BRL (sem sinal de menos)', () => {
    expect(/^R\$/.test(Money.format(-1234.5))).toBe(true);
    expect(/-/.test(Money.format(-1234.5))).toBe(false);
    expect(/1\.234,50/.test(Money.format(-1234.5))).toBe(true);
  });

  it('formatShort abrevia valores >= 1000 com "k"', () => {
    expect(Money.formatShort(2500)).toBe('R$ 2,5k');
    expect(/^R\$/.test(Money.formatShort(500))).toBe(true);
  });

  it('parse lê formato BR ("1.234,56") de volta pra número', () => {
    expect(Money.parse('1.234,56')).toBe(1234.56);
    expect(Money.parse('R$ 1.234,56')).toBe(1234.56);
    expect(Money.parse(100)).toBe(100);
  });

  it('colorBySign mapeia negativo pra expense, resto pra income', () => {
    expect(Money.colorBySign(-10)).toBe('var(--expense)');
    expect(Money.colorBySign(10)).toBe('var(--income)');
    expect(Money.colorBySign(0)).toBe('var(--income)');
  });

  it('isPositive/isNegative/abs', () => {
    expect(Money.isPositive(5)).toBe(true);
    expect(Money.isPositive(-5)).toBe(false);
    expect(Money.isNegative(-5)).toBe(true);
    expect(Money.abs(-42)).toBe(42);
  });
});

import { describe, expect, it } from 'vitest';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as PayableDomain from '@/feature/accounts-payable/domain.ts';

describe('feature:accounts-payable — domain', () => {
  it('isActive é false só quando status é cancelled', () => {
    expect(PayableDomain.isActive({ status: 'pending' })).toBe(true);
    expect(PayableDomain.isActive({ status: 'confirmed' })).toBe(true);
    expect(PayableDomain.isActive({ status: 'cancelled' })).toBe(false);
    expect(PayableDomain.isActive(null)).toBe(false);
  });

  it('isReceivable/isPayable distinguem pelo type', () => {
    expect(PayableDomain.isReceivable({ type: 'RECEIVABLE' })).toBe(true);
    expect(PayableDomain.isPayable({ type: 'RECEIVABLE' })).toBe(false);
    expect(PayableDomain.isPayable({ type: 'PAYABLE' })).toBe(true);
  });

  it('natureOf deriva a natureza da transação a partir do type', () => {
    expect(PayableDomain.natureOf('RECEIVABLE')).toBe('income');
    expect(PayableDomain.natureOf('PAYABLE')).toBe('expense');
    expect(PayableDomain.natureOf({ type: 'RECEIVABLE' })).toBe('income');
  });

  it('statusBadgeVariant mapeia status pro variant do badge', () => {
    expect(PayableDomain.statusBadgeVariant('confirmed')).toBe('income');
    expect(PayableDomain.statusBadgeVariant('scheduled')).toBe('warning');
    expect(PayableDomain.statusBadgeVariant('cancelled')).toBe('muted');
    expect(PayableDomain.statusBadgeVariant('pending')).toBe('expense');
  });

  it('periodTotals soma payable/receivable só de itens ativos dentro do período', () => {
    const period = Period.create(3, 2026);
    // due no dia 1º evitado de propósito: containsDate faz `new Date(due)`, string ISO sem hora
    // vira meia-noite UTC — em fuso de offset negativo isso rola pro mês anterior.
    const items = [
      { type: 'PAYABLE', amount: 100, due: '2026-03-10', status: 'pending' },
      { type: 'RECEIVABLE', amount: 200, due: '2026-03-15', status: 'pending' },
      { type: 'PAYABLE', amount: 50, due: '2026-03-20', status: 'cancelled' }, // inativo, ignorado
      { type: 'PAYABLE', amount: 999, due: '2026-04-15', status: 'pending' }, // fora do período
    ];
    const totals = PayableDomain.periodTotals(items, period);
    expect(totals).toEqual({ payable: 100, receivable: 200, result: 100 });
  });
});

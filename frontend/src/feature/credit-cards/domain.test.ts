import { describe, expect, it } from 'vitest';
import type { Account } from '../../core/kernel/_0_domain/account.ts';
import * as Period from '../../core/kernel/_0_domain/period.ts';
import * as CreditCardDomain from './domain.ts';

describe('feature:credit-cards — domain', () => {
  it('usagePct clampa em [0,100] e não divide por zero', () => {
    expect(CreditCardDomain.usagePct(500, 1000)).toBe(50);
    expect(CreditCardDomain.usagePct(1500, 1000)).toBe(100);
    expect(CreditCardDomain.usagePct(500, 0)).toBe(0);
  });

  it('availableCredit nunca fica negativo', () => {
    expect(CreditCardDomain.availableCredit(1000, 300)).toBe(700);
    expect(CreditCardDomain.availableCredit(1000, 1500)).toBe(0);
  });

  it('barColorByUsage mapeia faixas de uso pros tokens de cor', () => {
    expect(CreditCardDomain.barColorByUsage(90)).toBe('expense');
    expect(CreditCardDomain.barColorByUsage(70)).toBe('warning');
    expect(CreditCardDomain.barColorByUsage(30)).toBe('accent');
  });

  it('closingDay/dueDay caem pro default quando a conta não configura', () => {
    expect(CreditCardDomain.closingDay({} as Account)).toBe(CreditCardDomain.DEFAULT_CLOSING_DAY);
    expect(CreditCardDomain.dueDay({ dueDay: 15 } as Account)).toBe(15);
  });

  it('invoiceTotal soma só despesas do cartão dentro do ciclo da fatura', () => {
    const account = { id: '1', closingDay: 20, dueDay: 5 } as Account;
    const period = Period.create(4, 2026); // fatura de abril cobre 21/02–20/03
    const txs: CreditCardDomain.InvoiceTx[] = [
      { cardId: '9', date: '2026-03-10', amount: -100, type: 'expense' }, // dentro do ciclo
      { cardId: '9', date: '2026-03-25', amount: -999, type: 'expense' }, // fora do ciclo (após fechamento)
      { cardId: '8', date: '2026-03-10', amount: -50, type: 'expense' }, // outro cartão, ignorado
    ];
    expect(CreditCardDomain.invoiceTotal(txs, '9', account, period)).toBe(100);
  });
});

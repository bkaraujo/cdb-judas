import { describe, expect, it } from 'vitest';
import * as Account from '@/core/kernel/_0_domain/account.ts';

describe('kernel:account', () => {
  it('normalize aplica defaults e mapeia cards', () => {
    const a = Account.normalize({
      id: '1',
      name: 'Corrente',
      balance: '150.5',
      cards: [{ id: '9', last4: '1234', accountId: '1' }],
    });
    expect(a?.type).toBe('CHECKING');
    expect(a?.balance).toBe(150.5);
    expect(a?.currentBalance).toBe(150.5);
    expect(a?.active).toBe(true);
    expect(a?.cards.length).toBe(1);
    expect(a?.cards[0]?.last4).toBe('1234');
  });

  it('normalize retorna null pra payload vazio', () => {
    expect(Account.normalize(null)).toBe(null);
  });

  it('currentBalance prioriza currentBalance sobre balance', () => {
    expect(Account.currentBalance({ balance: 100, currentBalance: 80 } as Account.Account)).toBe(80);
    expect(Account.currentBalance({ balance: 100 } as Account.Account)).toBe(100);
    expect(Account.currentBalance(null)).toBe(0);
  });

  it('hasCards só é true com array não vazio', () => {
    expect(Account.hasCards({ cards: [{ id: '1' }] } as Account.Account)).toBe(true);
    expect(Account.hasCards({ cards: [] } as unknown as Account.Account)).toBe(false);
    expect(Account.hasCards({} as Account.Account)).toBe(false);
    expect(Account.hasCards(null)).toBe(false);
  });
});

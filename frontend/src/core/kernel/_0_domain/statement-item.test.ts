import { describe, expect, it } from 'vitest';
import * as StatementItem from '@/core/kernel/_0_domain/statement-item.ts';

describe('kernel:statement-item', () => {
  it('buildRows abre com o header "Saldo anterior" e acumula o saldo corrente', () => {
    const rows = StatementItem.buildRows(
      1000,
      [
        { id: '1', date: '2026-03-05', description: 'Compra', amount: -100, status: 'confirmed', categoryId: null },
        { id: '2', date: '2026-03-01', description: 'Salário', amount: 500, status: 'confirmed', categoryId: null },
      ],
      '2026-03-01',
    );

    expect(rows.length).toBe(3);
    expect(rows[0]?.status).toBe('balance');
    expect(rows[0]?.runningBal).toBe(1000);
    // Ordenadas por data ascendente: Salário (03-01) antes de Compra (03-05), mesmo entrando depois.
    expect(rows[1]?.id).toBe('2');
    expect(rows[1]?.runningBal).toBe(1500);
    expect(rows[2]?.id).toBe('1');
    expect(rows[2]?.runningBal).toBe(1400);
  });

  it('lista vazia devolve só o header', () => {
    const rows = StatementItem.buildRows(200, [], '2026-03-01');
    expect(rows.length).toBe(1);
    expect(rows[0]?.runningBal).toBe(200);
  });

  it('opts.headerBalance/startBalance suportam o cabeçalho de fatura de cartão', () => {
    const rows = StatementItem.buildRows(
      0,
      [{ id: '1', date: '2026-03-10', description: 'Compra', amount: -50, status: 'confirmed', categoryId: null }],
      null,
      { headerLabel: 'Fatura anterior', headerBalance: 300, startBalance: 0 },
    );

    expect(rows[0]?.description).toBe('Fatura anterior');
    expect(rows[0]?.runningBal).toBe(300);
    expect(rows[1]?.runningBal).toBe(-50);
  });

  it('isBalanceHeader aceita status como string ou como linha', () => {
    expect(StatementItem.isBalanceHeader('balance')).toBe(true);
    expect(StatementItem.isBalanceHeader({ status: 'balance' } as StatementItem.StatementRow)).toBe(true);
    expect(StatementItem.isBalanceHeader({ status: 'confirmed' } as StatementItem.StatementRow)).toBe(false);
  });

  it('runningBalance lê runningBal — buildRows é a única fonte de StatementRow e sempre o preenche', () => {
    // `row.balance` como fallback é remoção pré-aprovada (.claude/plan.md, Fase 3): o único
    // chamador real (statement-row.ts, Fase 5) só recebe linhas de buildRows, que nunca tem
    // `balance`, só `runningBal`.
    expect(StatementItem.runningBalance({ runningBal: 10 } as StatementItem.StatementRow)).toBe(10);
    expect(StatementItem.runningBalance(null)).toBe(0);
  });
});

/** Contrato do ciclo de fatura: docs/backend/invoice-cycle.md (espelhado no backend por
 * f002._0_domain.model.InvoiceCycle — mudou aqui, muda lá). */
import { describe, expect, it } from 'vitest';
import type { Account } from './account.ts';
import * as Invoice from './invoice.ts';
import * as Period from './period.ts';

// closingDay=20, dueDay=5 — mesmo exemplo do comentário de topo do arquivo fonte.
const ACCOUNT = {
  id: '1',
  name: 'Conta',
  closingDay: 20,
  dueDay: 5,
  cards: [{ id: '9', last4: '1234', accountId: '1', active: true }],
} as Account;

describe('kernel:invoice', () => {
  it('dueDate: compra antes do fechamento vence no mês seguinte', () => {
    expect(Invoice.dueDate(ACCOUNT, '2026-03-15')).toBe('2026-04-05');
  });

  it('dueDate: compra depois do fechamento empurra pro ciclo seguinte (vence 2 meses depois)', () => {
    expect(Invoice.dueDate(ACCOUNT, '2026-03-25')).toBe('2026-05-05');
  });

  it('cycleFor: fatura que vence 05/04 cobre 21/02–20/03 (exemplo documentado no fonte)', () => {
    expect(Invoice.cycleFor(ACCOUNT, '2026-04-05')).toEqual({ from: '2026-02-21', to: '2026-03-20' });
  });

  it('dueDatesIn devolve o vencimento único que cai dentro do período', () => {
    const dues = Invoice.dueDatesIn(ACCOUNT, Period.create(4, 2026));
    expect(dues).toEqual(['2026-04-05']);
  });

  it('label monta o texto sintético da linha de fatura', () => {
    expect(Invoice.dueDate(ACCOUNT, '2026-03-15')).toBe('2026-04-05');
  });

  it('collapse funde compras do mesmo cartão/vencimento numa linha só; transação sem cartão passa direto', () => {
    const period = Period.create(4, 2026); // Abril/2026
    const txs: Invoice.InvoiceTx[] = [
      { id: '100', cardId: '9', date: '2026-03-15', amount: -50 }, // vence 05/04
      { id: '101', cardId: '9', date: '2026-03-16', amount: -30 }, // mesma fatura
      { id: '102', cardId: null, date: '2026-04-10', amount: -20 }, // sem cartão
    ];
    const rows = Invoice.collapse(txs, [ACCOUNT], period);

    expect(rows.length).toBe(2);
    const invoiceRow = rows.find((r) => 'invoice' in r && r.invoice === true) as Invoice.InvoiceRow;
    expect(invoiceRow).toBeTruthy();
    expect(invoiceRow.amount).toBe(-80);
    expect(invoiceRow.date).toBe('2026-04-05');
    expect(invoiceRow.type).toBe('expense');

    const passthrough = rows.find((r) => !('invoice' in r) || !r.invoice) as Invoice.InvoiceTx;
    expect(passthrough.id).toBe('102');
  });

  it('mergeCards funde as faturas dos cartões da mesma conta numa linha só', () => {
    const rows: Invoice.CollapsedRow[] = [
      { id: 'invoice:9:2026-04-05', invoice: true, cardId: '9', accountId: '1', date: '2026-04-05', amount: -80, type: 'expense', status: 'scheduled', categoryId: null },
      { id: 'invoice:10:2026-04-05', invoice: true, cardId: '10', accountId: '1', date: '2026-04-05', amount: -20, type: 'expense', status: 'scheduled', categoryId: null },
      { id: '102', cardId: null, date: '2026-04-10', amount: -20 },
    ];
    const merged = Invoice.mergeCards(rows, [ACCOUNT]) as Invoice.InvoiceRow[];

    expect(merged.length).toBe(2);
    const cardsRow = merged[0] as Invoice.InvoiceRow;
    expect(cardsRow.description).toBe('Cartões Conta');
    expect(cardsRow.cards).toBe(true);
    expect(cardsRow.amount).toBe(-100);
    expect(cardsRow.date).toBe('2026-04-05');
    expect(cardsRow.cardId).toBe(null);
    expect(cardsRow.accountId).toBe('1');
    expect(cardsRow.status).toBe('scheduled');
    expect((merged[1] as unknown as Invoice.InvoiceTx).id).toBe('102');
  });

  it('mergeCards separa contas e vencimentos, e sem a lista de contas usa o rótulo genérico', () => {
    const rows: Invoice.CollapsedRow[] = [
      { id: 'a', invoice: true, cardId: '9', accountId: '1', date: '2026-04-05', amount: -80, type: 'expense', status: 'confirmed', categoryId: null },
      { id: 'b', invoice: true, cardId: '10', accountId: '2', date: '2026-04-05', amount: -50, type: 'expense', status: 'confirmed', categoryId: null },
      { id: 'c', invoice: true, cardId: '11', accountId: '1', date: '2026-04-15', amount: 30, type: 'income', status: 'scheduled', categoryId: null },
    ];
    const merged = Invoice.mergeCards(rows) as Invoice.InvoiceRow[];
    expect(merged.length).toBe(3);
    expect(merged[0]?.description).toBe('Cartões de crédito');
    expect(merged[2]?.type).toBe('income');
  });

  it('collapse ignora compra de cartão cujo vencimento cai fora do período', () => {
    const period = Period.create(3, 2026); // Março — vencimento (05/04) é no mês seguinte
    const txs: Invoice.InvoiceTx[] = [{ id: '100', cardId: '9', date: '2026-03-15', amount: -50 }];
    const rows = Invoice.collapse(txs, [ACCOUNT], period);
    expect(rows.length).toBe(0);
  });
});

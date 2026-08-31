import {describe, expect, it} from 'vitest';
import type {Account} from '@/core/kernel/_0_domain/account.ts';
import type {Category} from '@/core/kernel/_0_domain/category.ts';
import type {Tag} from '@/core/kernel/_0_domain/tag.ts';
import type {StatementRowLike} from '@/core/kernel/_2_infrastructure/primary/statement-row.ts';
import {
    rowCountLabel,
    statementColumns,
    statementRowHtml
} from '@/core/kernel/_2_infrastructure/primary/statement-row.ts';

function cat(id: string, name: string): Category {
  return { id, name, nature: 'EXPENSE', parentId: null, isSystem: false, active: true };
}
function acc(id: string, name: string): Account {
  return { id, name, type: 'CHECKING', balance: 0, currentBalance: 0, color: null, active: true, creditLimit: 0, overdraftLimit: 0, closingDay: null, dueDay: null, cards: [] };
}

const NO_TAGS: Tag[] = [];

describe('kernel:statement-row', () => {
  it('statementColumns mede a maior categoria/conta — puro, mesmo input produz o mesmo resultado', () => {
    const categories = [cat('1', 'Curta'), cat('2', 'Uma categoria bem mais comprida')];
    const accounts = [acc('1', 'Conta A'), acc('2', 'Conta Bem Mais Comprida')];

    const first = statementColumns(categories, accounts);
    expect(first.catColCh).toBeGreaterThan(20);
    expect(first.accColCh).toBeGreaterThan(15);

    const second = statementColumns(categories, accounts);
    expect(second).toEqual(first);
  });

  it('statementRowHtml estilo dot: linha de saldo anterior some o valor e mostra o acumulado', () => {
    const row: StatementRowLike = { id: null, date: '2026-03-01', description: 'Saldo anterior', amount: 0, status: 'balance', runningBal: 1000, categoryId: null, tagIds: [] };
    const cols = statementColumns([], []);
    const html = statementRowHtml(row, [], NO_TAGS, cols, { showBalance: true, status: 'dot', isLast: false });

    expect(html.indexOf('1.000,00')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('is-header')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('data-id=""')).toBeGreaterThanOrEqual(0);
  });

  it('statementRowHtml: showPurchaseDate troca a data da parcela pela data da compra', () => {
    const row: StatementRowLike = { id: '1', date: '2025-10-15', purchaseDate: '2025-07-15', description: 'Singer', amount: -72.99, status: 'confirmed', runningBal: -72.99, categoryId: null, tagIds: [], installmentNumber: 4, totalInstallments: 10 };
    const cols = statementColumns([], []);

    const withFlag = statementRowHtml(row, [], NO_TAGS, cols, { showBalance: true, status: 'dot', showPurchaseDate: true });
    expect(withFlag.indexOf('15 de jul.')).toBeGreaterThanOrEqual(0);
    expect(withFlag.indexOf('15 de out.')).toBe(-1);

    // Sem a flag (linha não parcelada) a data da compra é ignorada.
    const noFlag = statementRowHtml(row, [], NO_TAGS, cols, { showBalance: true, status: 'dot' });
    expect(noFlag.indexOf('15 de out.')).toBeGreaterThanOrEqual(0);
  });

  it('statementRowHtml: perna de transferência (categoria de sistema) ignora showPurchaseDate', () => {
    // Transferência nasce como grupo de 2 "parcelas" (1/2 saída, 2/2 entrada) — as telas ligam a
    // flag por totalInstallments, e a categoria de sistema é o que desfaz o engano.
    const transferCat: Category = { id: 'sys', name: 'Transferência', nature: 'EXPENSE', parentId: null, isSystem: true, active: true };
    const row: StatementRowLike = { id: '1', date: '2025-10-15', purchaseDate: '2025-07-15', description: 'Transferência (saída)', amount: -50, status: 'confirmed', runningBal: -50, categoryId: 'sys', tagIds: [], installmentNumber: 1, totalInstallments: 2 };
    const cols = statementColumns([transferCat], []);

    const html = statementRowHtml(row, [transferCat], NO_TAGS, cols, { showBalance: true, status: 'dot', showPurchaseDate: true });
    expect(html.indexOf('15 de out.')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('15 de jul.')).toBe(-1);
  });

  it('statementRowHtml estilo dot: transação comum mostra valor, acumulado e ações; isLast omite borda', () => {
    const categories = [cat('9', 'Mercado')];
    const row: StatementRowLike = { id: '5', date: '2026-03-10', description: 'Compra', amount: -80, status: 'pending', runningBal: 920, categoryId: '9', tagIds: [] };
    const cols = statementColumns(categories, []);

    const withBorder = statementRowHtml(row, categories, NO_TAGS, cols, { showBalance: true, status: 'dot', isLast: false });
    expect(withBorder.indexOf('is-last')).toBe(-1);

    const html = statementRowHtml(row, categories, NO_TAGS, cols, {
      showBalance: true, status: 'dot', isLast: true,
      actions: (r) => '<button data-act="edit" data-id="' + r.id + '"></button>',
    });
    expect(html.indexOf('80,00')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('920,00')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('data-act="edit"')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('is-last')).toBeGreaterThanOrEqual(0);
  });

  it('statementRowHtml esconde ações em linha de cabeçalho mesmo com actions() informado', () => {
    const row: StatementRowLike = { id: null, date: '2026-03-01', description: 'Fatura anterior', amount: 0, status: 'balance', runningBal: 300, categoryId: null, tagIds: [] };
    const cols = statementColumns([], []);
    const html = statementRowHtml(row, [], NO_TAGS, cols, {
      showBalance: true, status: 'dot',
      actions: () => '<button data-act="trash"></button>',
    });
    expect(html.indexOf('data-act="trash"')).toBe(-1);
  });

  it('statementRowHtml estilo badge: mostra índice, conta, status como badge e sempre o valor', () => {
    const row: StatementRowLike = { id: '3', date: '2026-03-05', description: 'Salário', amount: 500, status: 'confirmed', type: 'income', categoryId: null, tagIds: [] };
    const cols = statementColumns([], []);
    const html = statementRowHtml(row, [], NO_TAGS, cols, {
      index: 1, showAccount: true, accountName: 'Conta Corrente', status: 'badge',
    });

    expect(html.indexOf('>1</span>')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('Conta Corrente')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('class="badge badge-')).toBeGreaterThanOrEqual(0);
    expect(html.indexOf('500,00')).toBeGreaterThanOrEqual(0);
  });

  it('statementRowHtml invoiceLink: linka a descrição só quando invoiceLink e row.invoice são true', () => {
    const invoiceRow: StatementRowLike = { id: '7', date: '2026-03-05', description: 'Cartões de crédito', amount: -200, status: 'confirmed', invoice: true, categoryId: null, tagIds: [] };
    const cols = statementColumns([], []);

    const linked = statementRowHtml(invoiceRow, [], NO_TAGS, cols, { showBalance: true, status: 'dot', invoiceLink: true });
    expect(linked.indexOf('<a href="#/credit-cards"')).toBeGreaterThanOrEqual(0);

    const unlinked = statementRowHtml(invoiceRow, [], NO_TAGS, cols, { showBalance: true, status: 'dot', invoiceLink: false });
    expect(unlinked.indexOf('<a href="#/credit-cards"')).toBe(-1);
  });

  it('rowCountLabel: singular vs plural', () => {
    expect(rowCountLabel(1)).toBe('1 transação exibida');
    expect(rowCountLabel(0)).toBe('0 transações exibidas');
    expect(rowCountLabel(3)).toBe('3 transações exibidas');
  });
});

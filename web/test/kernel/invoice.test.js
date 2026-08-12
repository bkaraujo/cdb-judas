/* web/test/kernel/invoice.test.js — testes de core/kernel/_0_domain/invoice.js.
 * Contrato do ciclo de fatura: docs/backend/invoice-cycle.md (espelhado no backend por
 * f002._0_domain.model.InvoiceCycle — mudou aqui, muda lá). */
(function () {
  'use strict';

  // closingDay=20, dueDay=5 — mesmo exemplo do comentário de topo do arquivo fonte.
  const ACCOUNT = { id: 1, name: 'Conta', closingDay: 20, dueDay: 5, cards: [{ id: 9, last4: '1234', accountId: 1 }] };

  QUnit.module('kernel:invoice', function () {
    QUnit.test('dueDate: compra antes do fechamento vence no mês seguinte', function (assert) {
      assert.strictEqual(window.Domain.Invoice.dueDate(ACCOUNT, '2026-03-15'), '2026-04-05');
    });

    QUnit.test('dueDate: compra depois do fechamento empurra pro ciclo seguinte (vence 2 meses depois)', function (assert) {
      assert.strictEqual(window.Domain.Invoice.dueDate(ACCOUNT, '2026-03-25'), '2026-05-05');
    });

    QUnit.test('cycleFor: fatura que vence 05/04 cobre 21/02–20/03 (exemplo documentado no fonte)', function (assert) {
      assert.deepEqual(window.Domain.Invoice.cycleFor(ACCOUNT, '2026-04-05'), { from: '2026-02-21', to: '2026-03-20' });
    });

    QUnit.test('dueDatesIn devolve o vencimento único que cai dentro do período', function (assert) {
      const dues = window.Domain.Invoice.dueDatesIn(ACCOUNT, window.Domain.Period.create(4, 2026));
      assert.deepEqual(dues, ['2026-04-05']);
    });

    QUnit.test('label monta o texto sintético da linha de fatura', function (assert) {
      assert.strictEqual(
        window.Domain.Invoice.label(ACCOUNT, { last4: '1234' }),
        'FATURA · Conta 1234'
      );
    });

    QUnit.test('collapse funde compras do mesmo cartão/vencimento numa linha só; transação sem cartão passa direto', function (assert) {
      const period = window.Domain.Period.create(4, 2026); // Abril/2026
      const txs = [
        { id: 100, cardId: 9, accountId: 1, date: '2026-03-15', amount: -50, type: 'expense' }, // vence 05/04
        { id: 101, cardId: 9, accountId: 1, date: '2026-03-16', amount: -30, type: 'expense' }, // mesma fatura
        { id: 102, cardId: null, accountId: 1, date: '2026-04-10', amount: -20, type: 'expense' }, // sem cartão
      ];
      const rows = window.Domain.Invoice.collapse(txs, [ACCOUNT], period);

      assert.strictEqual(rows.length, 2, '2 compras de cartão viram 1 linha de fatura + 1 linha normal');
      const invoiceRow = rows.find(function (r) { return r.invoice === true; });
      assert.ok(invoiceRow, 'linha de fatura sintética presente');
      assert.strictEqual(invoiceRow.amount, -80, 'soma das duas compras (-50 + -30)');
      assert.strictEqual(invoiceRow.date, '2026-04-05', 'data da linha é o vencimento');
      assert.strictEqual(invoiceRow.type, 'expense', 'saldo negativo continua despesa');

      const passthrough = rows.find(function (r) { return !r.invoice; });
      assert.strictEqual(passthrough.id, 102, 'transação sem cartão dentro do período passa intacta');
    });

    QUnit.test('collapse ignora compra de cartão cujo vencimento cai fora do período', function (assert) {
      const period = window.Domain.Period.create(3, 2026); // Março — vencimento (05/04) é no mês seguinte
      const txs = [{ id: 100, cardId: 9, accountId: 1, date: '2026-03-15', amount: -50, type: 'expense' }];
      const rows = window.Domain.Invoice.collapse(txs, [ACCOUNT], period);
      assert.strictEqual(rows.length, 0, 'a compra só aparece no mês em que a fatura vence, não no mês da compra');
    });
  });
})();

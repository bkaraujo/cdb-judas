/* web/test/kernel/statement-item.test.js — testes de core/kernel/_0_domain/statement-item.js. */
(function () {
  'use strict';

  QUnit.module('kernel:statement-item', function () {
    QUnit.test('buildRows abre com o header "Saldo anterior" e acumula o saldo corrente', function (assert) {
      const rows = window.Domain.StatementItem.buildRows(1000, [
        { id: 1, date: '2026-03-05', description: 'Compra', amount: -100, status: 'confirmed' },
        { id: 2, date: '2026-03-01', description: 'Salário', amount: 500, status: 'confirmed' },
      ], '2026-03-01');

      assert.strictEqual(rows.length, 3, '1 header + 2 transações');
      assert.strictEqual(rows[0].status, 'balance');
      assert.strictEqual(rows[0].runningBal, 1000, 'header carrega o saldo de abertura');
      // Ordenadas por data ascendente: Salário (03-01) antes de Compra (03-05), mesmo entrando depois.
      assert.strictEqual(rows[1].id, 2);
      assert.strictEqual(rows[1].runningBal, 1500, '1000 + 500');
      assert.strictEqual(rows[2].id, 1);
      assert.strictEqual(rows[2].runningBal, 1400, '1500 - 100');
    });

    QUnit.test('lista vazia devolve só o header', function (assert) {
      const rows = window.Domain.StatementItem.buildRows(200, [], '2026-03-01');
      assert.strictEqual(rows.length, 1);
      assert.strictEqual(rows[0].runningBal, 200);
    });

    QUnit.test('opts.headerBalance/startBalance suportam o cabeçalho de fatura de cartão', function (assert) {
      const rows = window.Domain.StatementItem.buildRows(0, [
        { id: 1, date: '2026-03-10', description: 'Compra', amount: -50, status: 'confirmed' },
      ], null, { headerLabel: 'Fatura anterior', headerBalance: 300, startBalance: 0 });

      assert.strictEqual(rows[0].description, 'Fatura anterior');
      assert.strictEqual(rows[0].runningBal, 300, 'header mostra o total anterior');
      assert.strictEqual(rows[1].runningBal, -50, 'acumulado do ciclo atual recomeça do zero, não de 300');
    });

    QUnit.test('isBalanceHeader aceita status como string ou como linha', function (assert) {
      assert.true(window.Domain.StatementItem.isBalanceHeader('balance'));
      assert.true(window.Domain.StatementItem.isBalanceHeader({ status: 'balance' }));
      assert.false(window.Domain.StatementItem.isBalanceHeader({ status: 'confirmed' }));
    });

    QUnit.test('runningBalance prioriza runningBal sobre balance', function (assert) {
      assert.strictEqual(window.Domain.StatementItem.runningBalance({ runningBal: 10, balance: 20 }), 10);
      assert.strictEqual(window.Domain.StatementItem.runningBalance({ balance: 20 }), 20);
      assert.strictEqual(window.Domain.StatementItem.runningBalance(null), 0);
    });
  });
})();

/* web/test/kernel/transaction.test.js — testes de core/kernel/_0_domain/transaction.js. */
(function () {
  'use strict';

  QUnit.module('kernel:transaction', function () {
    QUnit.test('normalizeType mapeia aliases e cai pra expense por padrão', function (assert) {
      assert.strictEqual(window.Domain.Transaction.normalizeType('income'), 'income');
      assert.strictEqual(window.Domain.Transaction.normalizeType('revenue'), 'income', 'revenue é alias de income');
      assert.strictEqual(window.Domain.Transaction.normalizeType('transfer'), 'transfer');
      assert.strictEqual(window.Domain.Transaction.normalizeType('bogus'), 'expense', 'tipo desconhecido cai pra expense');
    });

    QUnit.test('signedAmount: negativo pra expense, positivo pros demais', function (assert) {
      assert.strictEqual(window.Domain.Transaction.signedAmount('expense', 100), -100);
      assert.strictEqual(window.Domain.Transaction.signedAmount('income', 100), 100);
      assert.strictEqual(window.Domain.Transaction.signedAmount('transfer', -50), 50, 'sempre valor absoluto primeiro');
    });

    QUnit.test('describe só acrescenta sufixo de parcela quando totalInstallments > 1', function (assert) {
      assert.strictEqual(
        window.Domain.Transaction.describe({ description: 'Amazon', totalInstallments: 3, installmentNumber: 2 }),
        'Amazon (2 de 3)'
      );
      assert.strictEqual(
        window.Domain.Transaction.describe({ description: 'Café', totalInstallments: 1, installmentNumber: 1 }),
        'Café',
        'compra à vista não recebe sufixo'
      );
      assert.strictEqual(window.Domain.Transaction.describe(null), '');
    });

    QUnit.test('isValidTransfer exige conta origem diferente da destino', function (assert) {
      assert.true(window.Domain.Transaction.isValidTransfer('a', 'b'));
      assert.false(window.Domain.Transaction.isValidTransfer('a', 'a'));
      assert.false(window.Domain.Transaction.isValidTransfer(null, 'b'));
    });

    QUnit.test('isToday compara contra data de referência injetada — determinístico', function (assert) {
      // isToday faz `new Date(t.date)` internamente — string ISO sem hora vira meia-noite UTC.
      // ref precisa ser construído do mesmo jeito (não new Date(y,m,d), que é meia-noite local)
      // pra não divergir em fusos com offset negativo (ex. America/Sao_Paulo).
      const ref = new Date('2026-08-11'); // injetado — sem depender de Date.now()
      assert.true(window.Domain.Transaction.isToday({ date: '2026-08-11' }, ref));
      assert.false(window.Domain.Transaction.isToday({ date: '2026-08-10' }, ref));
    });
  });
})();

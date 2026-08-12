/* web/test/feature/accounts-payable.test.js — testes de feature/accounts-payable.js.
 * Cobre Domain.Payable (puro) e o mapper puro App.PayableService.adapt(label). */
(function () {
  'use strict';

  QUnit.module('feature:accounts-payable — Domain.Payable', function () {
    QUnit.test('isActive é false só quando status é cancelled', function (assert) {
      assert.true(window.Domain.Payable.isActive({ status: 'pending' }));
      assert.true(window.Domain.Payable.isActive({ status: 'confirmed' }));
      assert.false(window.Domain.Payable.isActive({ status: 'cancelled' }));
      assert.false(window.Domain.Payable.isActive(null));
    });

    QUnit.test('isReceivable/isPayable distinguem pelo type', function (assert) {
      assert.true(window.Domain.Payable.isReceivable({ type: 'RECEIVABLE' }));
      assert.false(window.Domain.Payable.isPayable({ type: 'RECEIVABLE' }));
      assert.true(window.Domain.Payable.isPayable({ type: 'PAYABLE' }));
    });

    QUnit.test('natureOf deriva a natureza da transação a partir do type', function (assert) {
      assert.strictEqual(window.Domain.Payable.natureOf('RECEIVABLE'), 'income');
      assert.strictEqual(window.Domain.Payable.natureOf('PAYABLE'), 'expense');
      assert.strictEqual(window.Domain.Payable.natureOf({ type: 'RECEIVABLE' }), 'income', 'aceita objeto payable também');
    });

    QUnit.test('statusBadgeVariant mapeia status pro variant do badge', function (assert) {
      assert.strictEqual(window.Domain.Payable.statusBadgeVariant('confirmed'), 'income');
      assert.strictEqual(window.Domain.Payable.statusBadgeVariant('scheduled'), 'warning');
      assert.strictEqual(window.Domain.Payable.statusBadgeVariant('cancelled'), 'muted');
      assert.strictEqual(window.Domain.Payable.statusBadgeVariant('pending'), 'expense');
    });

    QUnit.test('periodTotals soma payable/receivable só de itens ativos dentro do período', function (assert) {
      const period = window.Domain.Period.create(3, 2026);
      // due no dia 1º evitado de propósito: containsDate faz `new Date(due)`, string ISO sem
      // hora vira meia-noite UTC — em fuso de offset negativo isso rola pro mês anterior.
      // Dias bem no meio do mês não têm esse risco de borda.
      const items = [
        { type: 'PAYABLE', amount: 100, due: '2026-03-10', status: 'pending' },
        { type: 'RECEIVABLE', amount: 200, due: '2026-03-15', status: 'pending' },
        { type: 'PAYABLE', amount: 50, due: '2026-03-20', status: 'cancelled' }, // inativo, ignorado
        { type: 'PAYABLE', amount: 999, due: '2026-04-15', status: 'pending' }, // fora do período
      ];
      const totals = window.Domain.Payable.periodTotals(items, period);
      assert.deepEqual(totals, { payable: 100, receivable: 200, result: 100 });
    });
  });

  QUnit.module('feature:accounts-payable — App.PayableService (repo fake)', {
    beforeEach: function () {
      // adapt(label) é helper privado da IIFE, nunca exportado a window.App.PayableService —
      // por isso testado aqui pela porta pública (listPayable/listReceivable + repo injetado),
      // não chamado direto (mesmo critério do padrão 009/docs/backend/testing.md: não testar
      // um helper que o consumidor nunca alcança).
      this.fakeRepo = {
        list: function (query) {
          return Promise.resolve([
            { id: 1, description: 'Aluguel', date: '2026-03-05', amount: -1200, accountId: 7, categoryId: 3, status: 'pending' },
          ]);
        },
      };
      window.App.PayableService.init({ repo: this.fakeRepo });
    }
  }, function () {
    QUnit.test('listPayable adapta transações pro formato da tela (Domain.Payable)', function (assert) {
      return window.App.PayableService.listPayable().then(function (out) {
        assert.strictEqual(out.length, 1);
        assert.strictEqual(out[0].due, '2026-03-05', 'date vira due');
        assert.strictEqual(out[0].amount, 1200, 'amount sempre positivo');
        assert.strictEqual(out[0].type, 'PAYABLE');
      });
    });

    QUnit.test('listReceivable usa o mesmo adapter com type RECEIVABLE', function (assert) {
      return window.App.PayableService.listReceivable().then(function (out) {
        assert.strictEqual(out[0].type, 'RECEIVABLE');
      });
    });
  });
})();

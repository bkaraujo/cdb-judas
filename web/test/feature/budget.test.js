/* web/test/feature/budget.test.js — testes de feature/budget.js.
 * Cobre Domain.Budget (puro) e App.BudgetService (via repo injetado).
 * Filosofia (docs/backend/testing.md): testar window.Domain.* / App.*Service pela porta
 * injetada — nunca um helper privado de IIFE, nunca código de DOM/render. */
(function () {
  'use strict';

  QUnit.module('feature:budget — Domain.Budget', function () {
    QUnit.test('consumptionPct clampa em [0,100] e não divide por zero', function (assert) {
      assert.strictEqual(window.Domain.Budget.consumptionPct(50, 200), 25);
      assert.strictEqual(window.Domain.Budget.consumptionPct(250, 200), 100, 'estoura clampa em 100');
      assert.strictEqual(window.Domain.Budget.consumptionPct(50, 0), 0, 'budgeted <= 0 não divide por zero');
    });

    QUnit.test('isOverBudget só é true com budgeted positivo e spent maior', function (assert) {
      assert.true(window.Domain.Budget.isOverBudget(250, 200));
      assert.false(window.Domain.Budget.isOverBudget(150, 200));
      assert.false(window.Domain.Budget.isOverBudget(250, 0), 'budgeted <= 0 nunca é "over"');
    });

    QUnit.test('overspendCount soma só os itens estourados', function (assert) {
      const items = [{ spent: 250, budgeted: 200 }, { spent: 100, budgeted: 200 }, { spent: 300, budgeted: 100 }];
      assert.strictEqual(window.Domain.Budget.overspendCount(items), 2);
      assert.strictEqual(window.Domain.Budget.overspendCount([]), 0);
    });

    QUnit.test('barColor mapeia faixas de consumo pros tokens de cor', function (assert) {
      assert.strictEqual(window.Domain.Budget.barColor(90), 'expense');
      assert.strictEqual(window.Domain.Budget.barColor(70), 'warning');
      assert.strictEqual(window.Domain.Budget.barColor(30), 'accent');
    });
  });

  QUnit.module('feature:budget — App.BudgetService (repo fake)', {
    beforeEach: function () {
      this.fakeRepo = {
        list:   function (m, y) { return Promise.resolve([{ id: 1, spent: 50, budgeted: 100, month: m, year: y }]); },
        create: function (data) { return Promise.resolve(Object.assign({ id: 99 }, data)); },
        update: function (id, data) { return Promise.resolve(Object.assign({ id: id }, data)); },
        remove: function (id) { return Promise.resolve({ id: id }); },
      };
      window.App.BudgetService.init({ repo: this.fakeRepo });
    }
  }, function () {
    QUnit.test('loadPeriod delega pro repo.list com month/year do período', function (assert) {
      return window.App.BudgetService.loadPeriod({ month: 3, year: 2026 }).then(function (items) {
        assert.strictEqual(items[0].month, 3);
        assert.strictEqual(items[0].year, 2026);
      });
    });

    QUnit.test('summary agrega total e overspending via Domain.Budget', function (assert) {
      const summary = window.App.BudgetService.summary([{ spent: 250, budgeted: 200 }, { spent: 50, budgeted: 200 }]);
      assert.deepEqual(summary, { total: 2, overspending: 1 });
    });

    QUnit.test('save/create/remove passam pelo repo injetado', function (assert) {
      return window.App.BudgetService.create({ name: 'Mercado', budgeted: 300 })
        .then(function (created) {
          assert.strictEqual(created.id, 99);
          return window.App.BudgetService.save(created.id, { budgeted: 350 });
        })
        .then(function (saved) {
          assert.strictEqual(saved.budgeted, 350);
          return window.App.BudgetService.remove(saved.id);
        })
        .then(function (removed) {
          assert.strictEqual(removed.id, 99);
        });
    });
  });
})();

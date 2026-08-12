/* web/test/feature/dashboard.test.js — testes de feature/dashboard.js (última fatia, lê de
 * quase todas as outras). Cobre Domain.BalanceSheet + Domain.DashboardAggregations (puros) e
 * App.DashboardService (via repo+txRepo fakes). */
(function () {
  'use strict';

  QUnit.module('feature:dashboard — Domain.BalanceSheet', function () {
    QUnit.test('compute soma o saldo corrente de todas as contas (sem passivo no modelo atual)', function (assert) {
      const sheet = window.Domain.BalanceSheet.compute([{ currentBalance: 100 }, { currentBalance: -30 }]);
      assert.deepEqual(sheet, { assets: 70, liabilities: 0, equity: 70 });
    });

    QUnit.test('lista vazia devolve tudo zerado', function (assert) {
      assert.deepEqual(window.Domain.BalanceSheet.compute([]), { assets: 0, liabilities: 0, equity: 0 });
    });
  });

  QUnit.module('feature:dashboard — Domain.DashboardAggregations', function () {
    const CATS = [{ id: 1, name: 'Mercado', nature: 'EXPENSE' }, { id: 2, name: 'Salário', nature: 'INCOME' }];

    QUnit.test('txIsExpense usa o type quando presente, senão a natureza da categoria, senão o sinal', function (assert) {
      assert.true(window.Domain.DashboardAggregations.txIsExpense({ type: 'expense' }));
      assert.false(window.Domain.DashboardAggregations.txIsExpense({ type: 'income' }));
      assert.true(window.Domain.DashboardAggregations.txIsExpense({}, 'EXPENSE'), 'sem type, cai pra natureza da categoria');
      assert.true(window.Domain.DashboardAggregations.txIsExpense({ amount: -50 }), 'sem type nem natureza, cai pro sinal');
    });

    QUnit.test('categoryNameFor/categoryNatureFor resolvem contra a lista de categorias', function (assert) {
      assert.strictEqual(window.Domain.DashboardAggregations.categoryNameFor({ categoryId: 1 }, CATS), 'Mercado');
      assert.strictEqual(window.Domain.DashboardAggregations.categoryNameFor({ categoryId: 999 }, CATS), 'Sem categoria');
      assert.strictEqual(window.Domain.DashboardAggregations.categoryNatureFor({ categoryId: 1 }, CATS), 'EXPENSE');
    });

    QUnit.test('expenseByCategory soma |amount| por categoria só das despesas, ordenado desc', function (assert) {
      const out = window.Domain.DashboardAggregations.expenseByCategory([
        { type: 'expense', categoryId: 1, amount: -100 },
        { type: 'expense', categoryId: 1, amount: -50 },
        { type: 'income', categoryId: 2, amount: 500 }, // receita, ignorada
      ], CATS);
      assert.deepEqual(out, [{ name: 'Mercado', value: 150 }]);
    });

    QUnit.test('topN corta a lista', function (assert) {
      assert.deepEqual(window.Domain.DashboardAggregations.topN([1, 2, 3, 4, 5, 6], 3), [1, 2, 3]);
      assert.strictEqual(window.Domain.DashboardAggregations.topN([1, 2], 5).length, 2);
    });

    QUnit.test('monthlySeries agrupa receitas/despesas por mês, com "now" injetado — determinístico', function (assert) {
      const now = new Date(2026, 7, 11); // 11/08/2026 local — injetado, sem Date.now()
      const series = window.Domain.DashboardAggregations.monthlySeries([
        { date: '2026-06-15', type: 'expense', amount: -100 }, // meio do mês, sem risco de fuso
        { date: '2026-08-15', type: 'income', amount: 500 },
      ], 3, now);

      assert.strictEqual(series.length, 3, 'jun/jul/ago');
      assert.deepEqual(series.map(function (b) { return b.month; }), [6, 7, 8]);
      assert.strictEqual(series[0].despesas, 100, 'junho');
      assert.strictEqual(series[2].receitas, 500, 'agosto');
    });

    QUnit.test('upcomingPayables filtra ativos (via AccountsPayableApi.isActive) e ordena por vencimento', function (assert) {
      const out = window.Domain.DashboardAggregations.upcomingPayables([
        { id: 1, status: 'pending', due: '2026-03-20' },
        { id: 2, status: 'cancelled', due: '2026-03-10' }, // inativo, excluído
        { id: 3, status: 'confirmed', due: '2026-03-05' },
      ]);
      assert.deepEqual(out.map(function (p) { return p.id; }), [3, 1]);
    });
  });

  QUnit.module('feature:dashboard — App.DashboardService (repo/txRepo fakes)', {
    beforeEach: function () {
      this.fakeRepo = {
        getMonthlyResult: function (month, year) { return Promise.resolve({ month: month, year: year, receitas: 0, despesas: 0 }); },
        getRecentTransactions: function (limit) { return Promise.resolve([{ id: 1, limit: limit }]); },
      };
      this.fakeTxRepo = {
        list: function (query) {
          if (query.indexOf('status=pending&type=expense') === 0) {
            return Promise.resolve([{ id: 1, date: '2026-03-05', description: 'Aluguel', amount: -1200, accountId: 1, categoryId: 3, status: 'pending' }]);
          }
          if (query.indexOf('status=pending&type=income') === 0) return Promise.resolve([]);
          return Promise.resolve([{ id: 2, date: '2026-03-10', amount: -50 }]);
        },
      };
      window.App.DashboardService.init({ repo: this.fakeRepo, txRepo: this.fakeTxRepo });
    }
  }, function () {
    QUnit.test('monthlyResult/recentTransactions delegam pro repo', function (assert) {
      return window.App.DashboardService.monthlyResult({ month: 3, year: 2026 })
        .then(function (r) {
          assert.strictEqual(r.month, 3);
          return window.App.DashboardService.recentTransactions(7);
        })
        .then(function (r) {
          assert.strictEqual(r[0].limit, 7);
        });
    });

    QUnit.test('loadAll busca em paralelo e adapta pendências pro formato payable/receivable', function (assert) {
      return window.App.DashboardService.loadAll(window.Domain.Period.create(3, 2026)).then(function (out) {
        assert.strictEqual(out.transactions.length, 1);
        assert.strictEqual(out.payables.length, 1);
        assert.strictEqual(out.payables[0].type, 'PAYABLE');
        assert.strictEqual(out.payables[0].due, '2026-03-05', 'date do repo vira due');
        assert.strictEqual(out.payables[0].amount, 1200, 'amount sempre positivo');
        assert.deepEqual(out.receivables, []);
      });
    });
  });
})();

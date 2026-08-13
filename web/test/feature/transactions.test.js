/* web/test/feature/transactions.test.js — testes de feature/transactions.js.
 * Domain.Transaction é kernel (já coberto em test/kernel/transaction.test.js); aqui
 * App.TransactionService: buildPayload (puro) e o restante via repo+cache fakes. */
(function () {
  'use strict';

  QUnit.module('feature:transactions — App.TransactionService.buildPayload (puro)', function () {
    QUnit.test('aplica a regra de sinal do domínio (expense negativa, income positiva)', function (assert) {
      const payload = window.App.TransactionService.buildPayload({ type: 'expense', amount: 150, description: 'Mercado' });
      assert.strictEqual(payload.amount, -150);
      assert.strictEqual(payload.type, 'expense');
      assert.strictEqual(payload.description, 'Mercado', 'demais campos do form preservados');
    });

    QUnit.test('normaliza o type (ex.: "revenue" vira "income")', function (assert) {
      const payload = window.App.TransactionService.buildPayload({ type: 'revenue', amount: 100 });
      assert.strictEqual(payload.type, 'income');
      assert.strictEqual(payload.amount, 100);
    });
  });

  QUnit.module('feature:transactions — App.TransactionService (repo/cache fakes)', {
    beforeEach: function () {
      this.fakeRepo = {
        list:           function (params) { return Promise.resolve([{ id: 1, description: 'Tx', params: params }]); },
        listByAccount:  function () { return Promise.resolve([]); },
        create:         function (data) { return Promise.resolve(Object.assign({ id: 99 }, data)); },
        update:         function (id, data) { return Promise.resolve(Object.assign({ id: id }, data)); },
        remove:         function (accountId, id) { return Promise.resolve({ accountId: accountId, id: id }); },
        patchStatus:    function (accountId, id, status) { return Promise.resolve({ accountId: accountId, id: id, status: status }); },
        transfer:       function (data) { return Promise.resolve(Object.assign({ id: 77 }, data)); },
      };
      this.fakeCache = { accounts: function () { return []; } };
      window.App.TransactionService.init({ repo: this.fakeRepo, cache: this.fakeCache });
    }
  }, function () {
    QUnit.test('list delega pro repo com os params recebidos', function (assert) {
      return window.App.TransactionService.list('status=pending').then(function (out) {
        assert.strictEqual(out[0].params, 'status=pending');
      });
    });

    QUnit.test('create/update/patchStatus/remove passam pelo repo', function (assert) {
      return window.App.TransactionService.create({ description: 'Nova' })
        .then(function (created) {
          assert.strictEqual(created.id, 99);
          return window.App.TransactionService.patchStatus(1, created.id, 'confirmed', '2026-03-10');
        })
        .then(function (patched) {
          assert.strictEqual(patched.status, 'confirmed');
          return window.App.TransactionService.remove(1, patched.id);
        })
        .then(function (removed) {
          assert.strictEqual(removed.id, 99);
        });
    });

    QUnit.test('listForPeriod colapsa via Domain.Invoice usando as contas do cache', function (assert) {
      // Sem contas com cartão no cache: fetchWindow devolve a janela do próprio mês, sem
      // alargar — collapse não tem nada pra colapsar, só repassa o que o repo devolveu.
      return window.App.TransactionService.listForPeriod(window.Domain.Period.create(3, 2026)).then(function (out) {
        assert.true(Array.isArray(out.rows));
        assert.true(Array.isArray(out.raw));
      });
    });

    QUnit.test('listForPeriod entrega uma linha "Cartões <conta>" por conta, com as compras cruas em raw', function (assert) {
      const account = {
        id: 1, name: 'Nubank', closingDay: 20, dueDay: 5,
        cards: [{ id: 9, last4: '1234', accountId: 1 }, { id: 10, last4: '5678', accountId: 1 }],
      };
      window.App.TransactionService.init({
        repo: Object.assign({}, this.fakeRepo, {
          list: function () {
            return Promise.resolve([
              { id: 1, cardId: 9,  accountId: 1, date: '2026-03-10', amount: -50, type: 'expense' },
              { id: 2, cardId: 10, accountId: 1, date: '2026-03-11', amount: -30, type: 'expense' },
            ]);
          },
        }),
        cache: { accounts: function () { return [account]; } },
      });
      return window.App.TransactionService.listForPeriod(window.Domain.Period.create(4, 2026)).then(function (out) {
        assert.strictEqual(out.rows.length, 1, 'os dois cartões da conta viram uma linha só');
        assert.strictEqual(out.rows[0].description, 'Cartões Nubank');
        assert.strictEqual(out.rows[0].amount, -80);
        assert.strictEqual(out.raw.length, 2, 'raw mantém as compras cruas pros modais de editar/excluir');
      });
    });
  });
})();

/* web/test/feature/statement.test.js — testes de feature/statement.js.
 * Sem domain própria (usa Domain.Period/Invoice/StatementItem/Account do kernel, já cobertos)
 * — aqui só App.StatementService, via txRepo+balance+cache fakes (init({txRepo,balance,cache})). */
(function () {
  'use strict';

  const ACCOUNT = { id: 1, name: 'Conta', balance: 999, cards: [] };

  QUnit.module('feature:statement — App.StatementService (fakes)', {
    beforeEach: function () {
      this.fakeTxRepo = {
        listByAccount: function () {
          return Promise.resolve([{ id: 1, date: '2026-03-10', description: 'Compra', amount: -50, status: 'confirmed' }]);
        },
      };
      this.fakeBalance = {
        allAccounts: function (period) {
          // Fecha o mês corrente com 500; o mês anterior (usado como abertura) não aparece
          // na resposta de propósito, pra exercitar o fallback pro saldo bruto da conta.
          return Promise.resolve(period.month === 3 ? [{ accountId: 1, balance: 500 }] : []);
        },
      };
      this.fakeCache = {
        accounts: function () { return [ACCOUNT]; },
        findById: function (type, id) { return type === 'accounts' && id === 1 ? ACCOUNT : null; },
      };
      window.App.StatementService.init({ txRepo: this.fakeTxRepo, balance: this.fakeBalance, cache: this.fakeCache });
    }
  }, function () {
    QUnit.test('load monta o header "Saldo anterior" + as transações com saldo corrente', function (assert) {
      return window.App.StatementService.load(1, window.Domain.Period.create(3, 2026), 100).then(function (rows) {
        assert.strictEqual(rows.length, 2, '1 header + 1 transação');
        assert.strictEqual(rows[0].status, 'balance');
        assert.strictEqual(rows[0].runningBal, 100, 'header carrega a abertura recebida');
        assert.strictEqual(rows[1].runningBal, 50, '100 - 50');
      });
    });

    QUnit.test('load devolve lista vazia se a conta não existe no cache', function (assert) {
      return window.App.StatementService.load(999, window.Domain.Period.create(3, 2026), 0).then(function (rows) {
        assert.deepEqual(rows, []);
      });
    });

    QUnit.test('summary combina o fechamento do período com a abertura (fechamento do mês anterior)', function (assert) {
      return window.App.StatementService.summary(window.Domain.Period.create(3, 2026)).then(function (out) {
        assert.strictEqual(out.length, 1);
        assert.strictEqual(out[0].closingBalance, 500, 'veio da resposta de balance pro mês corrente');
        assert.strictEqual(out[0].openingBalance, 999, 'mês anterior sem snapshot: cai pro balance bruto da conta');
      });
    });
  });
})();

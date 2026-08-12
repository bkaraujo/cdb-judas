/* web/test/feature/credit-cards.test.js — testes de feature/credit-cards.js.
 * Cobre Domain.CreditCard (puro) e App.CreditCardService (via txRepo+cache fakes). */
(function () {
  'use strict';

  QUnit.module('feature:credit-cards — Domain.CreditCard', function () {
    QUnit.test('usagePct clampa em [0,100] e não divide por zero', function (assert) {
      assert.strictEqual(window.Domain.CreditCard.usagePct(500, 1000), 50);
      assert.strictEqual(window.Domain.CreditCard.usagePct(1500, 1000), 100, 'estoura clampa em 100');
      assert.strictEqual(window.Domain.CreditCard.usagePct(500, 0), 0, 'limite <= 0 não divide por zero');
    });

    QUnit.test('availableCredit nunca fica negativo', function (assert) {
      assert.strictEqual(window.Domain.CreditCard.availableCredit(1000, 300), 700);
      assert.strictEqual(window.Domain.CreditCard.availableCredit(1000, 1500), 0, 'uso maior que o limite clampa em 0');
    });

    QUnit.test('barColorByUsage mapeia faixas de uso pros tokens de cor', function (assert) {
      assert.strictEqual(window.Domain.CreditCard.barColorByUsage(90), 'expense');
      assert.strictEqual(window.Domain.CreditCard.barColorByUsage(70), 'warning');
      assert.strictEqual(window.Domain.CreditCard.barColorByUsage(30), 'accent');
    });

    QUnit.test('closingDay/dueDay caem pro default quando a conta não configura', function (assert) {
      assert.strictEqual(window.Domain.CreditCard.closingDay({}), window.Domain.CreditCard.DEFAULT_CLOSING_DAY);
      assert.strictEqual(window.Domain.CreditCard.dueDay({ dueDay: 15 }), 15);
    });

    QUnit.test('invoiceTotal soma só despesas do cartão dentro do ciclo da fatura', function (assert) {
      const account = { id: 1, closingDay: 20, dueDay: 5 };
      const period = window.Domain.Period.create(4, 2026); // fatura de abril cobre 21/02–20/03
      const txs = [
        { cardId: 9, date: '2026-03-10', amount: -100, type: 'expense' }, // dentro do ciclo
        { cardId: 9, date: '2026-03-25', amount: -999, type: 'expense' }, // fora do ciclo (após fechamento)
        { cardId: 8, date: '2026-03-10', amount: -50, type: 'expense' },  // outro cartão, ignorado
      ];
      assert.strictEqual(window.Domain.CreditCard.invoiceTotal(txs, 9, account, period), 100);
    });
  });

  QUnit.module('feature:credit-cards — App.CreditCardService (txRepo/cache fakes)', {
    beforeEach: function () {
      this.fakeCache = {
        accounts: function () {
          return [
            { id: 1, name: 'Com cartão', cards: [{ id: 9, last4: '1234', accountId: 1 }] },
            { id: 2, name: 'Sem cartão', cards: [] },
          ];
        }
      };
      this.fakeTxRepo = {
        listByAccount: function () { return Promise.resolve([{ id: 1, cardId: 9, date: '2026-03-10', amount: -100, type: 'expense' }]); },
      };
      window.App.CreditCardService.init({ txRepo: this.fakeTxRepo, cache: this.fakeCache });
    }
  }, function () {
    QUnit.test('listFromCache achata os cartões de todas as contas, carregando a conta dona', function (assert) {
      const out = window.App.CreditCardService.listFromCache();
      assert.strictEqual(out.length, 1);
      assert.strictEqual(out[0].last4, '1234');
      assert.strictEqual(out[0].account.name, 'Com cartão');
    });

    QUnit.test('accountsWithCards filtra só contas que têm ao menos um cartão', function (assert) {
      const out = window.App.CreditCardService.accountsWithCards();
      assert.strictEqual(out.length, 1);
      assert.strictEqual(out[0].name, 'Com cartão');
    });

    QUnit.test('invoiceFor busca a janela do ciclo e filtra pelo cardId', function (assert) {
      const account = { id: 1, closingDay: 20, dueDay: 5 };
      const card = { id: 9, last4: '1234' };
      const period = window.Domain.Period.create(4, 2026);
      return window.App.CreditCardService.invoiceFor(card, account, period).then(function (invoice) {
        assert.strictEqual(invoice.dueDate, '2026-04-05');
        assert.strictEqual(invoice.transactions.length, 1);
        assert.strictEqual(invoice.total, 100);
      });
    });
  });
})();

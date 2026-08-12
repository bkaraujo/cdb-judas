/* web/test/kernel/account.test.js — testes de core/kernel/_0_domain/account.js. */
(function () {
  'use strict';

  QUnit.module('kernel:account', function () {
    QUnit.test('normalize aplica defaults e mapeia cards', function (assert) {
      const a = window.Domain.Account.normalize({
        id: 1, name: 'Corrente', balance: '150.5',
        cards: [{ id: 9, last4: '1234', accountId: 1 }],
      });
      assert.strictEqual(a.type, 'CHECKING', 'default type');
      assert.strictEqual(a.balance, 150.5);
      assert.strictEqual(a.currentBalance, 150.5, 'sem currentBalance no payload, cai pro balance');
      assert.true(a.active, 'default active');
      assert.strictEqual(a.cards.length, 1);
      assert.strictEqual(a.cards[0].last4, '1234');
    });

    QUnit.test('normalize retorna null pra payload vazio', function (assert) {
      assert.strictEqual(window.Domain.Account.normalize(null), null);
    });

    QUnit.test('currentBalance prioriza currentBalance sobre balance', function (assert) {
      assert.strictEqual(window.Domain.Account.currentBalance({ balance: 100, currentBalance: 80 }), 80);
      assert.strictEqual(window.Domain.Account.currentBalance({ balance: 100 }), 100, 'sem currentBalance, cai pro balance');
      assert.strictEqual(window.Domain.Account.currentBalance(null), 0);
    });

    QUnit.test('hasCards só é true com array não vazio', function (assert) {
      assert.true(window.Domain.Account.hasCards({ cards: [{ id: 1 }] }));
      assert.false(window.Domain.Account.hasCards({ cards: [] }));
      assert.false(window.Domain.Account.hasCards({}));
      assert.false(window.Domain.Account.hasCards(null));
    });
  });
})();

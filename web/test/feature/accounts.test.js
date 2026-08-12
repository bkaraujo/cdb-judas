/* web/test/feature/accounts.test.js — testes de feature/accounts.js.
 * Domain.Account é kernel (já coberto em test/kernel/account.test.js); aqui só
 * App.AccountService, via repo+cache fakes (init({repo, cache})). */
(function () {
  'use strict';

  QUnit.module('feature:accounts — App.AccountService (repo/cache fakes)', {
    beforeEach: function () {
      this.fakeRepo = {
        list:   function () { return Promise.resolve([{ id: 1, name: 'Conta' }]); },
        create: function (data) { return Promise.resolve(Object.assign({ id: 99 }, data)); },
        update: function (id, data) { return Promise.resolve(Object.assign({ id: id }, data)); },
        remove: function (id) { return Promise.resolve({ id: id }); },
        createCard: function (accountId, data) { return Promise.resolve(Object.assign({ id: 5, accountId: accountId }, data)); },
        removeCard: function (accountId, cardId) { return Promise.resolve({ accountId: accountId, cardId: cardId }); },
        setCardActive: function (accountId, cardId, active) { return Promise.resolve({ accountId: accountId, cardId: cardId, active: active }); },
      };
      this.fakeCache = {
        accounts: function () { return [{ id: 1, name: 'Conta (cache)' }]; },
        findById: function (type, id) { return type === 'accounts' ? { id: id, name: 'Achada' } : null; },
        subscribe: function () { return function unsubscribe() {}; },
      };
      window.App.AccountService.init({ repo: this.fakeRepo, cache: this.fakeCache });
    }
  }, function () {
    QUnit.test('list delega pro repo (rede)', function (assert) {
      return window.App.AccountService.list().then(function (out) {
        assert.strictEqual(out[0].name, 'Conta');
      });
    });

    QUnit.test('listCached lê do cache, sem rede', function (assert) {
      assert.strictEqual(window.App.AccountService.listCached()[0].name, 'Conta (cache)');
    });

    QUnit.test('findById delega pro cache com o tipo "accounts"', function (assert) {
      assert.strictEqual(window.App.AccountService.findById(7).name, 'Achada');
    });

    QUnit.test('create/update/remove passam pelo repo', function (assert) {
      return window.App.AccountService.create({ name: 'Nova' })
        .then(function (created) {
          assert.strictEqual(created.id, 99);
          return window.App.AccountService.update(created.id, { name: 'Renomeada' });
        })
        .then(function (updated) {
          assert.strictEqual(updated.name, 'Renomeada');
          return window.App.AccountService.remove(updated.id);
        })
        .then(function (removed) {
          assert.strictEqual(removed.id, 99);
        });
    });

    QUnit.test('addCard/removeCard/setCardActive delegam pro repo', function (assert) {
      return window.App.AccountService.addCard(1, { last4: '5678' }).then(function (card) {
        assert.strictEqual(card.last4, '5678');
        return window.App.AccountService.setCardActive(1, card.id, false);
      }).then(function (r) {
        assert.strictEqual(r.active, false);
      });
    });

    QUnit.test('onChange se inscreve no cache (ACCOUNT)', function (assert) {
      const unsubscribe = window.App.AccountService.onChange(function () {});
      assert.strictEqual(typeof unsubscribe, 'function');
    });
  });
})();

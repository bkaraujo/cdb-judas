/* web/test/feature/categories.test.js — testes de feature/categories.js.
 * Domain.Category é kernel (já coberto em test/kernel/category.test.js); aqui só
 * App.CategoryService, via repo+cache fakes. */
(function () {
  'use strict';

  const CATS = [
    { id: 1, name: 'Moradia', nature: 'EXPENSE', parentId: null, active: true },
    { id: 2, name: 'Aluguel', nature: 'EXPENSE', parentId: 1, active: true },
  ];

  QUnit.module('feature:categories — App.CategoryService (repo/cache fakes)', {
    beforeEach: function () {
      this.fakeRepo = {
        create: function (data) { return Promise.resolve(Object.assign({ id: 99 }, data)); },
        update: function (id, data) { return Promise.resolve(Object.assign({ id: id }, data)); },
        remove: function (id) { return Promise.resolve({ id: id }); },
      };
      this.fakeCache = {
        categories: function () { return CATS; },
        findById: function (type, id) { return type === 'categories' ? window.Domain.Category.byId(CATS, id) : null; },
        subscribe: function () { return function unsubscribe() {}; },
      };
      window.App.CategoryService.init({ repo: this.fakeRepo, cache: this.fakeCache });
    }
  }, function () {
    QUnit.test('listCached lê do cache', function (assert) {
      assert.strictEqual(window.App.CategoryService.listCached().length, 2);
    });

    QUnit.test('findById delega pro cache com o tipo "categories"', function (assert) {
      assert.strictEqual(window.App.CategoryService.findById(2).name, 'Aluguel');
    });

    QUnit.test('rootsByNature/childrenOf/labelChain/eligibleParents/isEffectivelyActive delegam pro Domain.Category com o cache', function (assert) {
      assert.strictEqual(window.App.CategoryService.rootsByNature('EXPENSE').length, 1);
      assert.strictEqual(window.App.CategoryService.childrenOf(1).length, 1);
      assert.strictEqual(window.App.CategoryService.labelChain(2), 'Moradia · Aluguel');
      assert.strictEqual(window.App.CategoryService.eligibleParents('EXPENSE', 1).length, 0);
      assert.true(window.App.CategoryService.isEffectivelyActive(2));
    });

    QUnit.test('create/update/remove passam pelo repo', function (assert) {
      return window.App.CategoryService.create({ name: 'Nova' })
        .then(function (created) {
          assert.strictEqual(created.id, 99);
          return window.App.CategoryService.update(created.id, { name: 'Renomeada' });
        })
        .then(function (updated) {
          assert.strictEqual(updated.name, 'Renomeada');
          return window.App.CategoryService.remove(updated.id);
        })
        .then(function (removed) {
          assert.strictEqual(removed.id, 99);
        });
    });

    QUnit.test('onChange se inscreve no cache (CATEGORY)', function (assert) {
      const unsubscribe = window.App.CategoryService.onChange(function () {});
      assert.strictEqual(typeof unsubscribe, 'function');
    });
  });
})();

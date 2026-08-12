/* web/test/feature/tags.test.js — testes de feature/tags.js.
 * Domain.Tag é kernel (já coberto em test/kernel/tag.test.js); aqui só App.TagService,
 * via repo+cache fakes. */
(function () {
  'use strict';

  QUnit.module('feature:tags — App.TagService (repo/cache fakes)', {
    beforeEach: function () {
      this.fakeRepo = {
        list:   function () { return Promise.resolve([{ id: 1, name: 'Viagem' }]); },
        create: function (data) { return Promise.resolve(Object.assign({ id: 99 }, data)); },
        update: function (id, data) { return Promise.resolve(Object.assign({ id: id }, data)); },
        remove: function (id) { return Promise.resolve({ id: id }); },
      };
      this.fakeCache = {
        tags: function () { return [{ id: 1, name: 'Viagem (cache)' }]; },
        findById: function (type, id) { return type === 'tags' ? { id: id, name: 'Achada' } : null; },
        subscribe: function () { return function unsubscribe() {}; },
      };
      window.App.TagService.init({ repo: this.fakeRepo, cache: this.fakeCache });
    }
  }, function () {
    QUnit.test('list delega pro repo (rede)', function (assert) {
      return window.App.TagService.list().then(function (out) {
        assert.strictEqual(out[0].name, 'Viagem');
      });
    });

    QUnit.test('listCached lê do cache, sem rede', function (assert) {
      assert.strictEqual(window.App.TagService.listCached()[0].name, 'Viagem (cache)');
    });

    QUnit.test('findById delega pro cache com o tipo "tags"', function (assert) {
      assert.strictEqual(window.App.TagService.findById(7).name, 'Achada');
    });

    QUnit.test('create/update/remove passam pelo repo', function (assert) {
      return window.App.TagService.create({ name: 'Nova' })
        .then(function (created) {
          assert.strictEqual(created.id, 99);
          return window.App.TagService.update(created.id, { name: 'Renomeada' });
        })
        .then(function (updated) {
          assert.strictEqual(updated.name, 'Renomeada');
          return window.App.TagService.remove(updated.id);
        })
        .then(function (removed) {
          assert.strictEqual(removed.id, 99);
        });
    });

    QUnit.test('onChange se inscreve no cache (TAG)', function (assert) {
      assert.strictEqual(typeof window.App.TagService.onChange(function () {}), 'function');
    });
  });
})();

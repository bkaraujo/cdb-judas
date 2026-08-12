/* web/test/feature/import-rules.test.js — testes de feature/import-rules.js.
 * Cobre Domain.ImportRuleMatcher (puro, usado idêntico no form manual e nos dois previews de
 * importação) e App.ImportRuleService (via repo+cache fakes). */
(function () {
  'use strict';

  QUnit.module('feature:import-rules — Domain.ImportRuleMatcher', function () {
    QUnit.test('normalize maiusculiza e remove acentos', function (assert) {
      assert.strictEqual(window.Domain.ImportRuleMatcher.normalize('Água Mineral'), 'AGUA MINERAL');
      assert.strictEqual(window.Domain.ImportRuleMatcher.normalize('  padaria  '), 'PADARIA');
      assert.strictEqual(window.Domain.ImportRuleMatcher.normalize(null), '');
    });

    QUnit.test('match acha a regra cujo nome é substring da descrição, ignorando acento/caixa', function (assert) {
      const rules = [{ name: 'Padaria' }, { name: 'Mercado' }];
      assert.strictEqual(window.Domain.ImportRuleMatcher.match('COMPRA PADARIA DO ZE', rules).name, 'Padaria');
      assert.strictEqual(window.Domain.ImportRuleMatcher.match('compra agua mineral', [{ name: 'Água' }]).name, 'Água');
      assert.strictEqual(window.Domain.ImportRuleMatcher.match('Nada a ver', rules), null);
    });

    QUnit.test('match resolve empate pela ordem do array (primeira regra que casa vence)', function (assert) {
      const rules = [{ name: 'Padaria' }, { name: 'Padaria do Zé' }];
      assert.strictEqual(window.Domain.ImportRuleMatcher.match('COMPRA PADARIA DO ZE', rules).name, 'Padaria');
    });

    QUnit.test('match com entrada vazia/sem regras devolve null sem lançar', function (assert) {
      assert.strictEqual(window.Domain.ImportRuleMatcher.match('', [{ name: 'X' }]), null);
      assert.strictEqual(window.Domain.ImportRuleMatcher.match('Compra', []), null);
      assert.strictEqual(window.Domain.ImportRuleMatcher.match('Compra', null), null);
    });
  });

  QUnit.module('feature:import-rules — App.ImportRuleService (repo/cache fakes)', {
    beforeEach: function () {
      this.fakeRepo = {
        list:   function () { return Promise.resolve([{ id: 1, name: 'Padaria' }]); },
        create: function (data) { return Promise.resolve(Object.assign({ id: 99 }, data)); },
        update: function (id, data) { return Promise.resolve(Object.assign({ id: id }, data)); },
        remove: function (id) { return Promise.resolve({ id: id }); },
      };
      this.fakeCache = {
        importRules: function () { return [{ id: 1, name: 'Padaria (cache)' }]; },
        findById: function (type, id) { return type === 'importRules' ? { id: id, name: 'Achada' } : null; },
        subscribe: function () { return function unsubscribe() {}; },
      };
      window.App.ImportRuleService.init({ repo: this.fakeRepo, cache: this.fakeCache });
    }
  }, function () {
    QUnit.test('list delega pro repo (rede)', function (assert) {
      return window.App.ImportRuleService.list().then(function (out) {
        assert.strictEqual(out[0].name, 'Padaria');
      });
    });

    QUnit.test('listCached lê do cache, sem rede', function (assert) {
      assert.strictEqual(window.App.ImportRuleService.listCached()[0].name, 'Padaria (cache)');
    });

    QUnit.test('findById delega pro cache com o tipo "importRules"', function (assert) {
      assert.strictEqual(window.App.ImportRuleService.findById(7).name, 'Achada');
    });

    QUnit.test('create/update/remove passam pelo repo', function (assert) {
      return window.App.ImportRuleService.create({ name: 'Nova' })
        .then(function (created) {
          assert.strictEqual(created.id, 99);
          return window.App.ImportRuleService.update(created.id, { name: 'Renomeada' });
        })
        .then(function (updated) {
          assert.strictEqual(updated.name, 'Renomeada');
          return window.App.ImportRuleService.remove(updated.id);
        })
        .then(function (removed) {
          assert.strictEqual(removed.id, 99);
        });
    });

    QUnit.test('onChange se inscreve no cache (IMPORT_RULE)', function (assert) {
      assert.strictEqual(typeof window.App.ImportRuleService.onChange(function () {}), 'function');
    });
  });
})();

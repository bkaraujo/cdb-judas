/* web/test/feature/cost-centers.test.js — testes de feature/cost-centers.js.
 * Cobre Domain.CostCenter (puro) e App.CostCenterService (via repo+cache fakes).
 * Centro de custo é dado fixo do sistema, somente leitura — sem create/update/remove. */
(function () {
  'use strict';

  QUnit.module('feature:cost-centers — Domain.CostCenter', function () {
    QUnit.test('normalize aplica defaults', function (assert) {
      const c = window.Domain.CostCenter.normalize({ id: 1, name: 'TI' });
      assert.strictEqual(c.name, 'TI');
      assert.strictEqual(c.description, '');
      assert.strictEqual(window.Domain.CostCenter.normalize(null), null);
    });

    QUnit.test('displaySubtitle só aparece quando description difere do name', function (assert) {
      assert.strictEqual(window.Domain.CostCenter.displaySubtitle({ name: 'TI', description: 'Tecnologia' }), 'Tecnologia');
      assert.strictEqual(window.Domain.CostCenter.displaySubtitle({ name: 'TI', description: 'TI' }), '', 'iguais, sem subtítulo');
      assert.strictEqual(window.Domain.CostCenter.displaySubtitle(null), '');
    });
  });

  QUnit.module('feature:cost-centers — App.CostCenterService (repo/cache fakes)', {
    beforeEach: function () {
      this.fakeRepo = { list: function () { return Promise.resolve([{ id: 1, name: 'TI' }]); } };
      this.fakeCache = {
        costCenters: function () { return [{ id: 1, name: 'TI (cache)' }]; },
        findById: function (type, id) { return type === 'costCenters' ? { id: id, name: 'Achado' } : null; },
        subscribe: function () { return function unsubscribe() {}; },
      };
      window.App.CostCenterService.init({ repo: this.fakeRepo, cache: this.fakeCache });
    }
  }, function () {
    QUnit.test('list delega pro repo (rede)', function (assert) {
      return window.App.CostCenterService.list().then(function (out) {
        assert.strictEqual(out[0].name, 'TI');
      });
    });

    QUnit.test('listCached lê do cache, sem rede', function (assert) {
      assert.strictEqual(window.App.CostCenterService.listCached()[0].name, 'TI (cache)');
    });

    QUnit.test('findById delega pro cache com o tipo "costCenters"', function (assert) {
      assert.strictEqual(window.App.CostCenterService.findById(7).name, 'Achado');
    });

    QUnit.test('onChange se inscreve no cache (COST_CENTER)', function (assert) {
      assert.strictEqual(typeof window.App.CostCenterService.onChange(function () {}), 'function');
    });
  });
})();

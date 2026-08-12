/* web/test/kernel/tag.test.js — testes de core/kernel/_0_domain/tag.js. */
(function () {
  'use strict';

  QUnit.module('kernel:tag', function () {
    QUnit.test('normalize aplica defaults', function (assert) {
      const t = window.Domain.Tag.normalize({ id: 1, name: 'Viagem' });
      assert.strictEqual(t.name, 'Viagem');
      assert.strictEqual(t.color, null);
      assert.strictEqual(window.Domain.Tag.normalize(null), null);
    });

    QUnit.test('hasColor', function (assert) {
      assert.true(window.Domain.Tag.hasColor({ color: '#fff' }));
      assert.false(window.Domain.Tag.hasColor({ color: null }));
      assert.false(window.Domain.Tag.hasColor(null));
    });

    QUnit.test('resolve mantém a ordem do catálogo e descarta ids órfãos', function (assert) {
      const catalog = [{ id: 1, name: 'A' }, { id: 2, name: 'B' }, { id: 3, name: 'C' }];
      const out = window.Domain.Tag.resolve([3, 1, 999], catalog);
      assert.deepEqual(out.map(function (t) { return t.id; }), [1, 3], 'ordem do catálogo, não da entrada; 999 descartado');
    });

    QUnit.test('resolve com entrada vazia/inválida devolve array vazio', function (assert) {
      assert.deepEqual(window.Domain.Tag.resolve([], []), []);
      assert.deepEqual(window.Domain.Tag.resolve(null, []), []);
    });
  });
})();

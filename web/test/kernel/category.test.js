/* web/test/kernel/category.test.js — testes de core/kernel/_0_domain/category.js. */
(function () {
  'use strict';

  const CATS = [
    { id: 1, name: 'Moradia', nature: 'EXPENSE', parentId: null, active: true },
    { id: 2, name: 'Aluguel', nature: 'EXPENSE', parentId: 1, active: true },
    { id: 3, name: 'Condomínio', nature: 'EXPENSE', parentId: 1, active: false },
    { id: 4, name: 'Salário', nature: 'INCOME', parentId: null, active: true },
  ];

  QUnit.module('kernel:category', function () {
    QUnit.test('normalize aplica defaults e deriva parentId de raw.parent quando ausente', function (assert) {
      const c = window.Domain.Category.normalize({ id: 1, name: 'X', parent: { id: 9 } });
      assert.strictEqual(c.parentId, 9);
      assert.strictEqual(c.nature, 'EXPENSE', 'default nature');
      assert.true(c.active, 'default active');
    });

    QUnit.test('isRoot é true só sem parentId', function (assert) {
      assert.true(window.Domain.Category.isRoot(CATS[0]));
      assert.false(window.Domain.Category.isRoot(CATS[1]));
    });

    QUnit.test('byId busca por id (comparação por string)', function (assert) {
      assert.strictEqual(window.Domain.Category.byId(CATS, 2).name, 'Aluguel');
      assert.strictEqual(window.Domain.Category.byId(CATS, '2').name, 'Aluguel', 'aceita id como string');
      assert.strictEqual(window.Domain.Category.byId(CATS, 999), null);
    });

    QUnit.test('childrenOf lista só os filhos diretos', function (assert) {
      const kids = window.Domain.Category.childrenOf(CATS, 1);
      assert.strictEqual(kids.length, 2);
      assert.deepEqual(kids.map(function (c) { return c.id; }), [2, 3]);
    });

    QUnit.test('labelChain monta "pai · filho" só pra subcategoria', function (assert) {
      assert.strictEqual(window.Domain.Category.labelChain(CATS, 2), 'Moradia · Aluguel');
      assert.strictEqual(window.Domain.Category.labelChain(CATS, 1), 'Moradia', 'raiz não tem prefixo');
      assert.strictEqual(window.Domain.Category.labelChain(CATS, 999), '');
    });

    QUnit.test('rootsByNature filtra raízes pela natureza', function (assert) {
      const roots = window.Domain.Category.rootsByNature(CATS, 'EXPENSE');
      assert.strictEqual(roots.length, 1);
      assert.strictEqual(roots[0].id, 1);
    });

    QUnit.test('isEffectivelyActive propaga inatividade do ancestral pro filho', function (assert) {
      assert.true(window.Domain.Category.isEffectivelyActive(CATS, 2), 'Aluguel e seu pai Moradia estão ativos');
      assert.false(window.Domain.Category.isEffectivelyActive(CATS, 3), 'Condomínio está inativo direto');
    });

    QUnit.test('eligibleParents exclui o próprio id', function (assert) {
      const eligible = window.Domain.Category.eligibleParents(CATS, 'EXPENSE', 1);
      assert.strictEqual(eligible.length, 0, 'única raiz EXPENSE é a excluída');
    });
  });
})();

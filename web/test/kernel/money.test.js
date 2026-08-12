/* web/test/kernel/money.test.js — testes de core/kernel/_0_domain/money.js. */
(function () {
  'use strict';

  QUnit.module('kernel:money', function () {
    QUnit.test('format sempre devolve valor absoluto em BRL (sem sinal de menos)', function (assert) {
      assert.true(/^R\$/.test(window.Domain.Money.format(-1234.5)));
      assert.false(/-/.test(window.Domain.Money.format(-1234.5)), 'nunca tem sinal de menos');
      assert.true(/1\.234,50/.test(window.Domain.Money.format(-1234.5)));
    });

    QUnit.test('formatShort abrevia valores >= 1000 com "k"', function (assert) {
      assert.strictEqual(window.Domain.Money.formatShort(2500), 'R$ 2,5k');
      assert.true(/^R\$/.test(window.Domain.Money.formatShort(500)), 'abaixo de 1000 usa o formato normal');
    });

    QUnit.test('parse lê formato BR ("1.234,56") de volta pra número', function (assert) {
      assert.strictEqual(window.Domain.Money.parse('1.234,56'), 1234.56);
      assert.strictEqual(window.Domain.Money.parse('R$ 1.234,56'), 1234.56);
      assert.strictEqual(window.Domain.Money.parse(100), 100, 'número passa direto');
    });

    QUnit.test('colorBySign mapeia negativo pra expense, resto pra income', function (assert) {
      assert.strictEqual(window.Domain.Money.colorBySign(-10), 'var(--expense)');
      assert.strictEqual(window.Domain.Money.colorBySign(10), 'var(--income)');
      assert.strictEqual(window.Domain.Money.colorBySign(0), 'var(--income)', 'zero não é negativo');
    });

    QUnit.test('isPositive/isNegative/abs', function (assert) {
      assert.true(window.Domain.Money.isPositive(5));
      assert.false(window.Domain.Money.isPositive(-5));
      assert.true(window.Domain.Money.isNegative(-5));
      assert.strictEqual(window.Domain.Money.abs(-42), 42);
    });
  });
})();

/* web/test/kernel/period.test.js — testes de core/kernel/_0_domain/period.js. */
(function () {
  'use strict';

  QUnit.module('kernel:period', function () {
    QUnit.test('create/shift navegam meses, virando o ano nas pontas', function (assert) {
      const p = window.Domain.Period.create(3, 2026);
      assert.deepEqual(window.Domain.Period.shift(p, 1), { month: 4, year: 2026 });
      assert.deepEqual(window.Domain.Period.shift(window.Domain.Period.create(12, 2026), 1), { month: 1, year: 2027 }, 'dezembro + 1 vira janeiro do ano seguinte');
      assert.deepEqual(window.Domain.Period.shift(window.Domain.Period.create(1, 2026), -1), { month: 12, year: 2025 }, 'janeiro - 1 vira dezembro do ano anterior');
    });

    QUnit.test('bounds devolve o primeiro e o último dia do mês (yyyy-mm-dd)', function (assert) {
      assert.deepEqual(window.Domain.Period.bounds(window.Domain.Period.create(2, 2028)), { from: '2028-02-01', to: '2028-02-29' }, '2028 é bissexto');
      assert.deepEqual(window.Domain.Period.bounds(window.Domain.Period.create(2, 2026)), { from: '2026-02-01', to: '2026-02-28' });
      assert.deepEqual(window.Domain.Period.bounds(window.Domain.Period.create(4, 2026)), { from: '2026-04-01', to: '2026-04-30' });
    });

    QUnit.test('yyyymm / yyyyDashMm formatam mês com zero à esquerda', function (assert) {
      assert.strictEqual(window.Domain.Period.yyyymm(window.Domain.Period.create(3, 2026)), '202603');
      assert.strictEqual(window.Domain.Period.yyyyDashMm(window.Domain.Period.create(3, 2026)), '2026-03');
    });

    QUnit.test('fromYyyyDashMm faz o parse inverso de yyyyDashMm', function (assert) {
      assert.deepEqual(window.Domain.Period.fromYyyyDashMm('2026-03'), { month: 3, year: 2026 });
      assert.strictEqual(window.Domain.Period.fromYyyyDashMm(null), null);
      assert.strictEqual(window.Domain.Period.fromYyyyDashMm('bogus'), null, 'sem separador válido devolve null');
    });

    QUnit.test('containsDate compara ano e mês, ignorando o dia', function (assert) {
      const p = window.Domain.Period.create(3, 2026);
      // Meio do mês, sem risco de borda de fuso (containsDate faz `new Date(dateLike)`, que
      // parseia string ISO como meia-noite UTC — dias 1 ou 31 poderiam rolar de mês em fuso
      // negativo; dia 15 nunca corre esse risco).
      assert.true(window.Domain.Period.containsDate(p, '2026-03-15'));
      assert.false(window.Domain.Period.containsDate(p, '2026-04-15'));
      assert.false(window.Domain.Period.containsDate(p, '2025-03-15'), 'ano diferente');
    });

    QUnit.test('equals compara mês e ano', function (assert) {
      assert.true(window.Domain.Period.equals(window.Domain.Period.create(3, 2026), window.Domain.Period.create(3, 2026)));
      assert.false(window.Domain.Period.equals(window.Domain.Period.create(3, 2026), window.Domain.Period.create(4, 2026)));
      assert.notOk(window.Domain.Period.equals(null, window.Domain.Period.create(3, 2026)), 'equals faz short-circuit e devolve null (falsy), não false');
    });
  });
})();

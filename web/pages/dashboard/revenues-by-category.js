/* pages/dashboard/revenues-by-category.js — Painel: Receitas por Categoria */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['revenues-cat'] = function (p, ctx) {
    var SERIES_PALETTE = ctx.SERIES_PALETTE;
    var map = {};
    ctx.currentMonthTxs().forEach(function (t) {
      if (ctx.txIsExpense(t)) return;
      var n = ctx.categoryName(t.categoryId);
      map[n] = (map[n] || 0) + Math.abs(+t.amount || 0);
    });
    var list = Object.keys(map).map(function (k, i) {
      return { name: k, amount: map[k], color: SERIES_PALETTE[i % SERIES_PALETTE.length] };
    });
    list.sort(function (a, b) { return b.amount - a.amount; });
    return ctx.panelWrap({ title: 'Receitas por Categoria', icon: p.icon, body: ctx.categoryBars(window.Domain.DashboardAggregations.topN(list, 5)) });
  };
})();

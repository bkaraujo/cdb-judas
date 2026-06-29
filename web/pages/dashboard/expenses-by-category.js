/* pages/dashboard/expenses-by-category.js — Painel: Despesas por Categoria */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['expenses-cat'] = function (p, ctx) {
    var data = ctx.expenseByCategory().slice(0, 5);
    return ctx.panelWrap({ title: 'Despesas por Categoria', icon: p.icon, body: ctx.categoryBars(data) });
  };
})();

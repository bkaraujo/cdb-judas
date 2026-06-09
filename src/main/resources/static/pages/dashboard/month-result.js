/* pages/dashboard/month-result.js — Painel: Resultado do Mês */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['month-result'] = function (p, ctx) {
    var txs = ctx.currentMonthTxs();
    var inc = 0, exp = 0;
    txs.forEach(function (t) {
      var a = Math.abs(+t.amount || 0);
      if (ctx.txIsExpense(t)) exp += a; else inc += a;
    });
    var res = inc - exp;
    var rows = [
      { label: 'Receitas', value: inc, color: 'var(--income)' },
      { label: 'Despesas', value: exp, color: 'var(--expense)' },
      { label: 'Resultado', value: res, color: res >= 0 ? 'var(--income)' : 'var(--expense)', bold: true },
    ];
    var body = '<div style="display:flex;flex-direction:column;gap:10px;">';
    rows.forEach(function (r) {
      body +=
        '<div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border-light);">' +
          '<span style="font-size:13px;color:var(--text-secondary);font-weight:' + (r.bold ? 700 : 400) + ';">' + ctx.esc(r.label) + '</span>' +
          '<span style="font-size:14px;font-weight:700;color:' + r.color + ';">' + ctx.esc(ctx.v(r.value)) + '</span>' +
        '</div>';
    });
    var series = ctx.monthlySeries(4);
    body += '<div style="height:80px;margin-top:4px;">' + ctx.miniLineChart(series, 80) + '</div>';
    body += '</div>';
    return ctx.panelWrap({ title: 'Resultado do Mês', icon: p.icon, body: body });
  };
})();

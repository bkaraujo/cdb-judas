/* pages/dashboard/cash-balances.js — Painel: Saldos de Caixa */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['cash-balances'] = function (p, ctx) {
    var accs = ctx.cashAccounts();
    var total = accs.reduce(function (s, a) { return s + window.Domain.Account.currentBalance(a); }, 0);
    var rows = accs.map(function (a, i) {
      var color = ctx.pickColor(i, a.color);
      var bal = window.Domain.Account.currentBalance(a);
      return (
        '<div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-top:1px solid var(--border-light);">' +
          '<div style="display:flex;align-items:center;gap:8px;min-width:0;">' +
            '<span style="width:8px;height:8px;border-radius:50%;background:' + ctx.esc(color) + ';flex-shrink:0;"></span>' +
            '<span style="font-size:12px;color:var(--text-secondary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' + ctx.esc(a.name) + '</span>' +
          '</div>' +
          '<span style="font-size:13px;font-weight:700;color:' + window.valueColor(bal) + ';">' + ctx.esc(ctx.v(bal)) + '</span>' +
        '</div>'
      );
    }).join('');
    if (!accs.length) {
      rows = '<div style="font-size:12px;color:var(--text-muted);text-align:center;padding:12px 0;">Sem contas cadastradas.</div>';
    }
    var body =
      '<div style="margin-bottom:12px;">' +
        '<p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase;letter-spacing:0.04em;">Total</p>' +
        '<p style="font-size:24px;font-weight:800;margin-top:2px;color:' + window.valueColor(total) + ';">' +
          ctx.esc(ctx.v(total)) +
        '</p>' +
      '</div>' + rows;
    return ctx.panelWrap({ title: 'Saldos de Caixa', icon: p.icon, body: body });
  };
})();

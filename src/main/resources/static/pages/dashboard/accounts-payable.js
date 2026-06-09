/* pages/dashboard/accounts-payable.js — Painéis: Contas a Pagar / Contas a Receber */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  function renderListaPayable(p, ctx, kind) {
    var isExp = kind === 'expense';
    var src = ctx.upcomingPayables(kind).slice(0, 6);
    var bodyEl;
    if (!src.length) {
      bodyEl = window.emptyState({
        icon: isExp ? 'calendar' : 'arrowUp',
        title: isExp ? 'Sem contas a pagar' : 'Sem contas a receber',
        desc: 'Tudo em dia.',
      });
    } else {
      var color = isExp ? 'var(--expense)' : 'var(--income)';
      var html = '<div style="display:flex;flex-direction:column;gap:0;">';
      src.forEach(function (b) {
        var due = b.due || b.date;
        var dueTxt = '';
        try { dueTxt = window.fmtDate(due); } catch (e) {}
        html +=
          '<div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border-light);">' +
            '<div style="min-width:0;">' +
              '<p style="font-size:12px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' +
                ctx.esc(b.description || b.name || '—') +
              '</p>' +
              '<p style="font-size:11px;color:var(--text-muted);">' + ctx.esc(dueTxt) + '</p>' +
            '</div>' +
            '<span style="font-size:13px;font-weight:700;color:' + color + ';flex-shrink:0;margin-left:10px;">' +
              ctx.esc(ctx.v(Math.abs(+b.amount || 0))) +
            '</span>' +
          '</div>';
      });
      html += '</div>';
      bodyEl = html;
    }
    var action = window.badge(String(ctx.upcomingPayables(kind).length), isExp ? 'expense' : 'income');
    return ctx.panelWrap({
      title: isExp ? 'Contas a Pagar' : 'Contas a Receber',
      icon: isExp ? 'calendar' : 'arrowUp',
      action: action,
      body: bodyEl,
    });
  }

  window.DashboardPanels['accounts-payable'] = function (p, ctx) {
    return renderListaPayable(p, ctx, 'expense');
  };

  window.DashboardPanels['accounts-receivable'] = function (p, ctx) {
    return renderListaPayable(p, ctx, 'income');
  };
})();

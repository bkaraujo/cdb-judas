/* pages/dashboard/recent-postings.js — Painel: Últimos Lançamentos */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['recent-postings'] = function (p, ctx) {
    var txs = (ctx.state.data.transactions || []).slice(0, 7);
    if (!txs.length) {
      return ctx.panelWrap({
        title: 'Últimos Lançamentos', icon: p.icon,
        body: window.emptyState({ icon: 'list', title: 'Sem lançamentos', desc: 'Nenhum movimento ainda.' }),
      });
    }
    var html = '';
    txs.forEach(function (t) {
      var isExp = ctx.txIsExpense(t);
      var color = isExp ? 'var(--expense)' : 'var(--income)';
      var amt   = Math.abs(+t.amount || 0);
      var dateTxt = '';
      try { dateTxt = window.fmtDate(t.date); } catch (e) {}
      var cat = ctx.categoryName(t.categoryId);
      html +=
        '<div style="display:flex;align-items:center;gap:10px;padding:7px 0;border-bottom:1px solid var(--border-light);">' +
          '<span style="width:28px;height:28px;border-radius:6px;background:' + (isExp ? 'var(--expense-light)' : 'var(--income-light)') +
            ';color:' + color + ';display:flex;align-items:center;justify-content:center;flex-shrink:0;">' +
            window.icon(isExp ? 'arrowDown' : 'arrowUp', 14) +
          '</span>' +
          '<div style="flex:1;min-width:0;">' +
            '<p style="font-size:12px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' +
              ctx.esc(t.description || '—') +
            '</p>' +
            '<p style="font-size:11px;color:var(--text-muted);">' + ctx.esc(cat) + (dateTxt ? ' · ' + ctx.esc(dateTxt) : '') + '</p>' +
          '</div>' +
          '<span style="font-size:12px;font-weight:700;color:' + color + ';flex-shrink:0;">' +
            ctx.esc(ctx.v(amt)) +
          '</span>' +
        '</div>';
    });
    var action = $(
      '<button class="btn btn-ghost btn-sm" data-act="goto-tx" type="button">' +
        '<span>Ver todos</span>' +
      '</button>'
    );
    return ctx.panelWrap({ title: 'Últimos Lançamentos', icon: p.icon, action: action, body: html });
  };
})();

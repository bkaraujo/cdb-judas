/* pages/dashboard/expense-goals.js — Painel: Metas de Despesa */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['expense-goals'] = function (p, ctx) {
    var now = new Date();
    var $card = ctx.panelWrap({
      title: 'Metas de Despesa', icon: p.icon,
      body: '<div style="font-size:12px;color:var(--text-muted);text-align:center;padding:20px 0;">Carregando…</div>',
    });
    window.App.BudgetService.loadPeriod(window.Domain.Period.create(now.getMonth() + 1, now.getFullYear())).then(function (list) {
      var data = (Array.isArray(list) ? list : []).filter(function (g) {
        return g && (g.budgeted || g.budget || g.target);
      });
      var $body = $card.find('div').last();
      if (!data.length) {
        $body.html(window.emptyState({ icon: 'target', title: 'Sem metas', desc: 'Configure metas em Metas / Orçamento.' }));
        return;
      }
      var html = '<div style="display:flex;flex-direction:column;gap:10px;">';
      data.slice(0, 6).forEach(function (g) {
        var name = g.name || g.categoryName || ctx.categoryName(g.categoryId) || 'Categoria';
        var budgeted = +(g.budgeted || g.budget || g.target || 0);
        var spent = +(g.spent || g.actual || 0);
        var pct = window.Domain.Budget.consumptionPct(spent, budgeted);
        var over = window.Domain.Budget.isOverBudget(spent, budgeted);
        var barColor = over ? 'var(--expense)' : pct > 80 ? 'var(--warning)' : 'var(--income)';
        html +=
          '<div>' +
            '<div style="display:flex;justify-content:space-between;margin-bottom:4px;font-size:12px;">' +
              '<span style="color:var(--text-secondary);">' + ctx.esc(name) + '</span>' +
              '<span style="font-weight:700;color:' + (over ? 'var(--expense)' : 'var(--text-primary)') + ';">' +
                ctx.esc(ctx.v(spent)) + ' / ' + ctx.esc(ctx.v(budgeted)) +
              '</span>' +
            '</div>' +
            '<div style="height:6px;background:var(--bg-hover);border-radius:3px;overflow:hidden;">' +
              '<div style="height:100%;border-radius:3px;width:' + pct + '%;background:' + barColor + ';transition:width 0.5s ease;"></div>' +
            '</div>' +
          '</div>';
      });
      html += '</div>';
      $body.html(html);
    }).catch(function () {
      var $body = $card.find('div').last();
      $body.html(window.emptyState({ icon: 'target', title: 'Em breve', desc: 'Endpoint de metas indisponível.' }));
    });
    return $card;
  };
})();

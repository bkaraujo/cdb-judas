/* pages/dashboard/credit-cards.js — Painel: Cartões de Crédito */
(function () {
  window.DashboardPanels = window.DashboardPanels || {};

  window.DashboardPanels['credit-cards'] = function (p, ctx) {
    var accounts = ctx.creditCards(); // accounts with at least one card
    if (!accounts.length) {
      return ctx.panelWrap({
        title: 'Cartões de Crédito', icon: p.icon,
        body: window.emptyState({ icon: 'creditCard', title: 'Sem cartões', desc: 'Cadastre um cartão para visualizar aqui.' }),
      });
    }
    var period = window.Domain.Period.currentMonth();
    var html = '';
    accounts.forEach(function (a, i) {
      var limit = a.creditLimit || 0;
      // Invoice total = every card on the account combined, matched by tx.cardId.
      // Usa a lista inteira (não currentMonthTxs): o ciclo da fatura que vence neste mês começa
      // no mês anterior — ver Domain.Invoice.
      var used = window.Domain.CreditCard.accountInvoiceTotal(ctx.allTxs(), a, period);
      var pct = window.Domain.CreditCard.usagePct(used, limit);
      var color = ctx.pickColor(i + 4, a.color);
      var barColor = 'var(--' + window.Domain.CreditCard.barColorByUsage(pct) + ')';
      html +=
        '<div style="margin-bottom:12px;">' +
          '<div style="display:flex;justify-content:space-between;margin-bottom:6px;">' +
            '<div style="display:flex;align-items:center;gap:6px;min-width:0;">' +
              '<span style="width:10px;height:10px;border-radius:2px;background:' + ctx.esc(color) + ';flex-shrink:0;"></span>' +
              '<span style="font-size:12px;font-weight:600;color:var(--text-primary);">' + ctx.esc(a.name) + '</span>' +
            '</div>' +
            '<span style="font-size:12px;color:var(--expense);font-weight:700;">' + ctx.esc(ctx.v(used)) + '</span>' +
          '</div>' +
          '<div style="height:6px;background:var(--bg-hover);border-radius:3px;overflow:hidden;margin-bottom:4px;">' +
            '<div style="height:100%;border-radius:3px;width:' + pct + '%;background:' + barColor + ';transition:width 0.5s ease;"></div>' +
          '</div>' +
          '<div style="display:flex;justify-content:space-between;font-size:11px;color:var(--text-muted);">' +
            '<span>' + (limit > 0 ? pct.toFixed(0) + '% usado' : 'Sem limite') + '</span>' +
            '<span>Limite: ' + ctx.esc(limit > 0 ? ctx.v(limit) : '—') + '</span>' +
          '</div>' +
        '</div>';
    });
    return ctx.panelWrap({ title: 'Cartões de Crédito', icon: p.icon, body: html });
  };
})();

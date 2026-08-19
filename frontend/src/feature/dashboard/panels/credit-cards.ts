/** Painel: Cartões de Crédito. */
import * as Period from '@/core/kernel/_0_domain/period.ts';
import { emptyState } from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { progressBarHtml } from '@/core/kernel/_2_infrastructure/primary/ui/progress-bar.ts';
import type { PanelRenderer } from '@/feature/dashboard/panels/types.ts';

export const creditCards: PanelRenderer = (p, ctx) => {
  const accounts = ctx.creditCards(); // contas com ao menos um cartão
  if (!accounts.length) {
    return ctx.panelWrap({
      title: 'Cartões de Crédito', icon: p.icon,
      body: emptyState({ icon: 'creditCard', title: 'Sem cartões', desc: 'Cadastre um cartão para visualizar aqui.' }),
    });
  }
  const period = Period.currentMonth();
  let html = '';
  accounts.forEach((a, i) => {
    const limit = a.creditLimit || 0;
    // Invoice total = todos os cartões da conta combinados, casado por tx.cardId. Usa a lista
    // inteira (não currentMonthTxs): o ciclo da fatura que vence neste mês começa no mês
    // anterior — ver Domain.Invoice (credit-cards).
    const used = ctx.creditCardsApi.accountInvoiceTotal(ctx.allTxs(), a, period);
    const pct = ctx.creditCardsApi.usagePct(used, limit);
    const color = ctx.pickColor(i + 4, a.color);
    const barColor = 'var(--' + ctx.creditCardsApi.barColorByUsage(pct) + ')';
    html +=
      '<div style="margin-bottom:12px;">' +
        '<div style="display:flex;justify-content:space-between;margin-bottom:6px;">' +
          '<div style="display:flex;align-items:center;gap:6px;min-width:0;">' +
            '<span style="width:10px;height:10px;border-radius:2px;background:' + ctx.esc(color) + ';flex-shrink:0;"></span>' +
            '<span style="font-size:12px;font-weight:600;color:var(--text-primary);">' + ctx.esc(a.name) + '</span>' +
          '</div>' +
          '<span style="font-size:12px;color:var(--expense);font-weight:700;">' + ctx.esc(ctx.v(used)) + '</span>' +
        '</div>' +
        progressBarHtml(pct, barColor, { size: 'sm', marginBottom: '4px' }) +
        '<div style="display:flex;justify-content:space-between;font-size:11px;color:var(--text-muted);">' +
          '<span>' + (limit > 0 ? pct.toFixed(0) + '% usado' : 'Sem limite') + '</span>' +
          '<span>Limite: ' + ctx.esc(limit > 0 ? ctx.v(limit) : '—') + '</span>' +
        '</div>' +
      '</div>';
  });
  return ctx.panelWrap({ title: 'Cartões de Crédito', icon: p.icon, body: html });
};

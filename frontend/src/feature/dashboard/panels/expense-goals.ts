/** Painel: Metas de Despesa. Único painel assíncrono: renderiza um esqueleto "Carregando…" e
 * substitui o corpo quando `BudgetApi.loadPeriod` resolve. `/budget` 404 hoje (ver fatia
 * `budget`) — o catch mostra "Em breve" em vez de erro, preservado do original. */
import * as Period from '../../../core/kernel/_0_domain/period.ts';
import { emptyState } from '../../../core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { progressBarHtml } from '../../../core/kernel/_2_infrastructure/primary/ui/progress-bar.ts';
import type { PanelRenderer } from './types.ts';

export const expenseGoals: PanelRenderer = (p, ctx) => {
  const now = new Date();
  const $card = ctx.panelWrap({
    title: 'Metas de Despesa', icon: p.icon,
    body: '<div style="font-size:12px;color:var(--text-muted);text-align:center;padding:20px 0;">Carregando…</div>',
  });
  ctx.budgetApi
    .loadPeriod(Period.create(now.getMonth() + 1, now.getFullYear()))
    .then((list) => {
      const data = (Array.isArray(list) ? list : []).filter((g) => g && g.budgeted);
      const $body = $card.find('div').last();
      if (!data.length) {
        $body.html(emptyState({ icon: 'target', title: 'Sem metas', desc: 'Configure metas em Metas / Orçamento.' }));
        return;
      }
      let html = '<div style="display:flex;flex-direction:column;gap:10px;">';
      data.slice(0, 6).forEach((g) => {
        // `.categoryName` nunca existe em BudgetResponse — fallback morto preservado do original.
        const loose = g as unknown as { categoryName?: string };
        const name = g.name || loose.categoryName || ctx.categoryName(g.categoryId) || 'Categoria';
        const budgeted = +(g.budgeted || 0);
        const spent = +(g.spent || 0);
        const pct = ctx.budgetApi.consumptionPct(spent, budgeted);
        const over = ctx.budgetApi.isOverBudget(spent, budgeted);
        const barColor = over ? 'var(--expense)' : pct > 80 ? 'var(--warning)' : 'var(--income)';
        html +=
          '<div>' +
            '<div style="display:flex;justify-content:space-between;margin-bottom:4px;font-size:12px;">' +
              '<span style="color:var(--text-secondary);">' + ctx.esc(name) + '</span>' +
              '<span style="font-weight:700;color:' + (over ? 'var(--expense)' : 'var(--text-primary)') + ';">' +
                ctx.esc(ctx.v(spent)) + ' / ' + ctx.esc(ctx.v(budgeted)) +
              '</span>' +
            '</div>' +
            progressBarHtml(pct, barColor, { size: 'sm' }) +
          '</div>';
      });
      html += '</div>';
      $body.html(html);
    })
    .catch(() => {
      const $body = $card.find('div').last();
      $body.html(emptyState({ icon: 'target', title: 'Em breve', desc: 'Endpoint de metas indisponível.' }));
    });
  return $card;
};

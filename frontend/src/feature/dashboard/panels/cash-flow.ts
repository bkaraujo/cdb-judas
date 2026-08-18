/** Painel: Fluxo de Caixa. */
import type { PanelRenderer } from './types.ts';

export const cashFlow: PanelRenderer = (p, ctx) => {
  const series = ctx.monthlySeries(6);
  const body =
    '<div style="display:flex;gap:16px;margin-bottom:10px;font-size:12px;">' +
      '<span style="display:flex;align-items:center;gap:4px;color:var(--text-secondary);">' +
        '<span style="width:14px;height:3px;border-radius:2px;background:var(--income);display:inline-block;"></span>Receitas' +
      '</span>' +
      '<span style="display:flex;align-items:center;gap:4px;color:var(--text-secondary);">' +
        '<span style="width:14px;height:3px;border-radius:2px;background:var(--expense);display:inline-block;"></span>Despesas' +
      '</span>' +
    '</div>' +
    '<div style="height:110px;">' + ctx.miniLineChart(series, 110) + '</div>';
  return ctx.panelWrap({ title: 'Fluxo de Caixa', icon: p.icon, body });
};

/** Painel: Resultado do Mês. */
import { valueColor } from '@/core/kernel/_0_domain/format.ts';
import type { PanelRenderer } from '@/feature/dashboard/panels/types.ts';

export const monthResult: PanelRenderer = (p, ctx) => {
  const txs = ctx.currentMonthTxs();
  let inc = 0;
  let exp = 0;
  txs.forEach((t) => {
    const a = Math.abs(Number(t.amount) || 0);
    if (ctx.txIsExpense(t)) exp += a;
    else inc += a;
  });
  const res = inc - exp;
  const rows = [
    { label: 'Receitas', value: inc, color: 'var(--income)', bold: false },
    { label: 'Despesas', value: exp, color: 'var(--expense)', bold: false },
    { label: 'Resultado', value: res, color: valueColor(res), bold: true },
  ];
  let body = '<div style="display:flex;flex-direction:column;gap:10px;">';
  rows.forEach((r) => {
    body +=
      '<div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border-light);">' +
        '<span style="font-size:13px;color:var(--text-secondary);font-weight:' + (r.bold ? 700 : 400) + ';">' + ctx.esc(r.label) + '</span>' +
        '<span style="font-size:14px;font-weight:700;color:' + r.color + ';">' + ctx.esc(ctx.v(r.value)) + '</span>' +
      '</div>';
  });
  const series = ctx.monthlySeries(4);
  body += '<div style="height:80px;margin-top:4px;">' + ctx.miniLineChart(series, 80) + '</div>';
  body += '</div>';
  return ctx.panelWrap({ title: 'Resultado do Mês', icon: p.icon, body });
};

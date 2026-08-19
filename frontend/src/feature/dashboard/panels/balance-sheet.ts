/** Painel: Balanço Patrimonial. */
import * as BalanceSheetDomain from '@/feature/dashboard/domain.ts';
import type { PanelRenderer } from '@/feature/dashboard/panels/types.ts';

export const balanceSheet: PanelRenderer = (p, ctx) => {
  const sheet = BalanceSheetDomain.compute(ctx.cbdAccounts());
  const rows = [
    { label: 'Ativo Total', value: sheet.assets, color: 'var(--income)', bold: false },
    { label: 'Passivo Total', value: sheet.liabilities, color: 'var(--expense)', bold: false },
    { label: 'Patrimônio Líq.', value: sheet.equity, color: 'var(--accent)', bold: true },
  ];
  let body = '<div style="display:flex;flex-direction:column;gap:8px;">';
  rows.forEach((r) => {
    body +=
      '<div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--border-light);">' +
        '<span style="font-size:13px;color:var(--text-secondary);font-weight:' + (r.bold ? 700 : 400) + ';">' + ctx.esc(r.label) + '</span>' +
        '<span style="font-size:14px;font-weight:700;color:' + r.color + ';">' + ctx.esc(ctx.v(r.value)) + '</span>' +
      '</div>';
  });
  body += '</div>';
  return ctx.panelWrap({ title: 'Balanço Patrimonial', icon: p.icon, body });
};

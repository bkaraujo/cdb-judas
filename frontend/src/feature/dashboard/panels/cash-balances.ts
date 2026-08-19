/** Painel: Saldos de Caixa. */
import { valueColor } from '@/core/kernel/_0_domain/format.ts';
import * as AccountDomain from '@/core/kernel/_0_domain/account.ts';
import type { PanelRenderer } from '@/feature/dashboard/panels/types.ts';

export const cashBalances: PanelRenderer = (p, ctx) => {
  const accs = ctx.cashAccounts();
  const total = accs.reduce((s, a) => s + AccountDomain.currentBalance(a), 0);
  let rows = accs
    .map((a, i) => {
      const color = ctx.pickColor(i, a.color);
      const bal = AccountDomain.currentBalance(a);
      return (
        '<div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-top:1px solid var(--border-light);">' +
          '<div style="display:flex;align-items:center;gap:8px;min-width:0;">' +
            '<span style="width:8px;height:8px;border-radius:50%;background:' + ctx.esc(color) + ';flex-shrink:0;"></span>' +
            '<span style="font-size:12px;color:var(--text-secondary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' + ctx.esc(a.name) + '</span>' +
          '</div>' +
          '<span style="font-size:13px;font-weight:700;color:' + valueColor(bal) + ';">' + ctx.esc(ctx.v(bal)) + '</span>' +
        '</div>'
      );
    })
    .join('');
  if (!accs.length) {
    rows = '<div style="font-size:12px;color:var(--text-muted);text-align:center;padding:12px 0;">Sem contas cadastradas.</div>';
  }
  const body =
    '<div style="margin-bottom:12px;">' +
      '<p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase;letter-spacing:0.04em;">Total</p>' +
      '<p style="font-size:24px;font-weight:800;margin-top:2px;color:' + valueColor(total) + ';">' + ctx.esc(ctx.v(total)) + '</p>' +
    '</div>' + rows;
  return ctx.panelWrap({ title: 'Saldos de Caixa', icon: p.icon, body });
};

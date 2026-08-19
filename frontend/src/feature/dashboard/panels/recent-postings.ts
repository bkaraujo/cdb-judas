/** Painel: Últimos Lançamentos. */
import $ from 'jquery';
import { fmtDate } from '@/core/kernel/_0_domain/format.ts';
import * as Transaction from '@/core/kernel/_0_domain/transaction.ts';
import { icon } from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import { emptyState } from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import type { PanelRenderer } from '@/feature/dashboard/panels/types.ts';

export const recentPostings: PanelRenderer = (p, ctx) => {
  const txs = (ctx.state.data.transactions || []).slice(0, 7);
  if (!txs.length) {
    return ctx.panelWrap({
      title: 'Últimos Lançamentos', icon: p.icon,
      body: emptyState({ icon: 'list', title: 'Sem lançamentos', desc: 'Nenhum movimento ainda.' }),
    });
  }
  let html = '';
  txs.forEach((t) => {
    const isExp = ctx.txIsExpense(t);
    const color = isExp ? 'var(--expense)' : 'var(--income)';
    const amt = Math.abs(Number(t.amount) || 0);
    let dateTxt = '';
    try {
      dateTxt = fmtDate(t.date);
    } catch {
      /* noop */
    }
    const cat = ctx.categoryName(t.categoryId);
    html +=
      '<div style="display:flex;align-items:center;gap:10px;padding:7px 0;border-bottom:1px solid var(--border-light);">' +
        '<span style="width:28px;height:28px;border-radius:6px;background:' + (isExp ? 'var(--expense-light)' : 'var(--income-light)') +
          ';color:' + color + ';display:flex;align-items:center;justify-content:center;flex-shrink:0;">' +
          icon(isExp ? 'arrowDown' : 'arrowUp', 14) +
        '</span>' +
        '<div style="flex:1;min-width:0;">' +
          '<p style="font-size:12px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;text-transform:uppercase;">' +
            ctx.esc((ctx.isTransferCategory(t.categoryId) ? t.description || '' : Transaction.describe(t)) || '—') +
          '</p>' +
          '<p style="font-size:11px;color:var(--text-muted);">' + ctx.esc(cat) + (dateTxt ? ' · ' + ctx.esc(dateTxt) : '') + '</p>' +
        '</div>' +
        '<span style="font-size:12px;font-weight:700;color:' + color + ';flex-shrink:0;">' + ctx.esc(ctx.v(amt)) + '</span>' +
      '</div>';
  });
  const action = $('<button class="btn btn-ghost btn-sm" data-act="goto-tx" type="button"><span>Ver todos</span></button>');
  return ctx.panelWrap({ title: 'Últimos Lançamentos', icon: p.icon, action, body: html });
};

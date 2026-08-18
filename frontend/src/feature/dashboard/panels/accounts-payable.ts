/** Painéis: Contas a Pagar / Contas a Receber. */
import { fmtDate } from '../../../core/kernel/_0_domain/format.ts';
import { badge } from '../../../core/kernel/_2_infrastructure/primary/ui/badge.ts';
import { emptyState } from '../../../core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import type { PanelRenderer } from './types.ts';

function renderListaPayable(_p: Parameters<PanelRenderer>[0], ctx: Parameters<PanelRenderer>[1], kind: 'expense' | 'income'): JQuery {
  const isExp = kind === 'expense';
  const src = ctx.upcomingPayables(kind).slice(0, 6);
  let bodyEl: string;
  if (!src.length) {
    bodyEl = emptyState({
      icon: isExp ? 'calendar' : 'arrowUp',
      title: isExp ? 'Sem contas a pagar' : 'Sem contas a receber',
      desc: 'Tudo em dia.',
    });
  } else {
    const color = isExp ? 'var(--expense)' : 'var(--income)';
    let html = '<div style="display:flex;flex-direction:column;gap:0;">';
    src.forEach((b) => {
      // `.name`/`.date` nunca existem em PayableListItem (só `description`/`due`) — fallback
      // morto preservado do original, sem efeito observável (description sempre presente).
      const loose = b as unknown as { name?: string; date?: string | null };
      const due = b.due || loose.date;
      let dueTxt = '';
      try {
        dueTxt = fmtDate(due);
      } catch {
        /* noop */
      }
      html +=
        '<div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border-light);">' +
          '<div style="min-width:0;">' +
            '<p style="font-size:12px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;text-transform:uppercase;">' +
              ctx.esc(b.description || loose.name || '—') +
            '</p>' +
            '<p style="font-size:11px;color:var(--text-muted);">' + ctx.esc(dueTxt) + '</p>' +
          '</div>' +
          '<span style="font-size:13px;font-weight:700;color:' + color + ';flex-shrink:0;margin-left:10px;">' +
            ctx.esc(ctx.v(Math.abs(Number(b.amount) || 0))) +
          '</span>' +
        '</div>';
    });
    html += '</div>';
    bodyEl = html;
  }
  const action = badge(String(ctx.upcomingPayables(kind).length), isExp ? 'expense' : 'income');
  return ctx.panelWrap({
    title: isExp ? 'Contas a Pagar' : 'Contas a Receber',
    icon: isExp ? 'calendar' : 'arrowUp',
    action,
    body: bodyEl,
  });
}

export const accountsPayable: PanelRenderer = (p, ctx) => renderListaPayable(p, ctx, 'expense');
export const accountsReceivable: PanelRenderer = (p, ctx) => renderListaPayable(p, ctx, 'income');

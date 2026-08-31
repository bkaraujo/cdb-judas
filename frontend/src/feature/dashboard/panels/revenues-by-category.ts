/** Painel: Receitas por Categoria. */
import * as DashboardAggregations from '@/feature/dashboard/domain.ts';
import type {PanelRenderer} from '@/feature/dashboard/panels/types.ts';

export const revenuesByCategory: PanelRenderer = (p, ctx) => {
  const map: Record<string, number> = {};
  ctx.currentMonthTxs().forEach((t) => {
    if (ctx.txIsExpense(t)) return;
    const n = ctx.categoryName(t.categoryId);
    map[n] = (map[n] || 0) + Math.abs(Number(t.amount) || 0);
  });
  const list = Object.keys(map).map((k, i) => ({
    name: k,
    amount: map[k] || 0,
    color: ctx.SERIES_PALETTE[i % ctx.SERIES_PALETTE.length] || ctx.SERIES_PALETTE[0] || '',
  }));
  list.sort((a, b) => b.amount - a.amount);
  return ctx.panelWrap({ title: 'Receitas por Categoria', icon: p.icon, body: ctx.categoryBars(DashboardAggregations.topN(list, 5)) });
};

/** Painel: Despesas por Categoria. */
import type { PanelRenderer } from '@/feature/dashboard/panels/types.ts';

export const expensesByCategory: PanelRenderer = (p, ctx) => {
  const data = ctx.expenseByCategory().slice(0, 5);
  return ctx.panelWrap({ title: 'Despesas por Categoria', icon: p.icon, body: ctx.categoryBars(data) });
};

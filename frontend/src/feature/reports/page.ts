/** pages/reports — stub. */
import { emptyState } from '../../core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { createPage } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '../../core/kernel/_2_infrastructure/primary/page.ts';

type ReportsPageState = PageState;

export function createReportsPage(): Page {
  let state: ReportsPageState | null = null;
  return createPage<ReportsPageState>({
    ns: '.reports',
    state: () => {
      state = {};
      return state;
    },
    render: () => {
      state?.$root?.html(emptyState({ icon: 'barChart', title: 'Em construção', desc: 'Relatórios será migrado em breve.' }));
    },
  });
}

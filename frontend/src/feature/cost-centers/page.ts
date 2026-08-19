import $ from 'jquery';
import { esc } from '@/core/kernel/_0_domain/format.ts';
import { icon } from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import { cachePage } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import { emptyState } from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import * as CostCenterDomain from '@/feature/cost-centers/domain.ts';
import type { CostCenter } from '@/feature/cost-centers/domain.ts';
import type { CostCenterService } from '@/feature/cost-centers/service.ts';

interface CostCentersPageState extends PageState {
  costCenters: CostCenter[];
}

function sortByDescription(a: CostCenter, b: CostCenter): number {
  const an = String(a.description || '').toLowerCase();
  const bn = String(b.description || '').toLowerCase();
  return an.localeCompare(bn, 'pt-BR');
}

function renderRow(cc: CostCenter): JQuery {
  const description = cc.description || '';
  const name = cc.name || '';
  const subtitle = CostCenterDomain.displaySubtitle(cc);
  const title = name || description;

  const avatarHtml =
    '<div style="width:36px;height:36px;border-radius:8px;flex-shrink:0;' +
    'background:var(--accent-light);color:var(--accent);' +
    'display:flex;align-items:center;justify-content:center;">' +
    icon('briefcase', 16) +
    '</div>';

  return $(
    '<div class="card-row" data-id="' + esc(cc.id) + '">' +
      '<div class="card-row-main">' +
        avatarHtml +
        '<div style="min-width:0;display:flex;flex-direction:column;">' +
          '<span class="row-title" style="color:var(--text-primary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + esc(title) + '</span>' +
          (subtitle ? '<span class="row-sub" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + esc(subtitle) + '</span>' : '') +
        '</div>' +
      '</div>' +
    '</div>',
  );
}

export function createCostCentersPage(service: CostCenterService): Page {
  let state: CostCentersPageState | null = null;

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;

    const $header = pageHeader({ title: 'Centros de Custo' });

    let $body: JQuery;
    if (!state.costCenters.length) {
      $body = $(emptyState({ icon: 'briefcase', title: 'Nenhum centro de custo disponível', desc: 'Os centros de custo são fixos do sistema.' }));
    } else {
      const sorted = state.costCenters.slice().sort(sortByDescription);
      $body = $('<div class="card card-list"></div>');
      sorted.forEach((cc) => $body.append(renderRow(cc)));
    }

    $root.empty().append($header).append($body);
  }

  // CostCenterService.onChange já existia mas nunca tinha sido conectado aqui — a tela não
  // reagia a mudanças de outras origens (ex. quick-create em import-rules). cachePage conecta.
  return cachePage<CostCentersPageState>({
    ns: '.cost-centers',
    state: () => {
      state = { costCenters: [] };
      return state;
    },
    sync: (s) => {
      s.costCenters = service.listCached();
    },
    subscribe: (cb) => service.onChange(cb),
    render,
  });
}

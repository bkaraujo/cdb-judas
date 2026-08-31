/** pages/reports — índice de relatórios disponíveis. */
import $ from 'jquery';
import {icon} from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import type {Page, PageState} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {createPage} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {pageHeader} from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';

type ReportsPageState = PageState;

const REPORTS = [
  { id: 'report-category-evolution', label: 'Evolução por categoria', desc: 'Transações agrupadas por categoria e período', icon: 'trendingUp' },
];

export function createReportsPage(): Page {
  let state: ReportsPageState | null = null;

  function render(): void {
    const $root = state?.$root;
    if (!$root) return;
    $root.empty();

    const $header = pageHeader({ title: 'Relatórios' });
    $root.append($header);

    const $grid = $('<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px;"></div>');
    REPORTS.forEach((report) => {
      const $card = $('<div class="card" style="display:flex;flex-direction:column;gap:12px;padding:20px;cursor:pointer;transition:all 200ms;" data-id="' + report.id + '"></div>');
      $card.html(
        '<div style="display:flex;align-items:center;gap:12px;">' +
          '<span style="width:40px;height:40px;border-radius:8px;background:var(--income-light);color:var(--income);display:inline-flex;align-items:center;justify-content:center;">' +
            icon(report.icon, 20) +
          '</span>' +
          '<span style="font-size:16px;font-weight:700;color:var(--text-primary);">' + report.label + '</span>' +
        '</div>' +
        '<p style="margin:0;font-size:13px;color:var(--text-muted);">' + report.desc + '</p>'
      );
      $card.on('mouseenter', function () {
        $(this).css('background', 'var(--bg-hover)');
      });
      $card.on('mouseleave', function () {
        $(this).css('background', '');
      });
      $card.on('click', function () {
        window.location.hash = '#/' + report.id;
      });
      $grid.append($card);
    });
    $root.append($grid);
  }

  return createPage<ReportsPageState>({
    ns: '.reports',
    state: () => {
      state = {};
      return state;
    },
    render,
  });
}

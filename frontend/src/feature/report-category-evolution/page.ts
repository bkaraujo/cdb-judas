/** pages/report-category-evolution — Matriz de transações agrupadas por categoria × período.
 * Seletores: Intervalo (mensal/anual), Início, Tamanho, Carregar. Clique em macro expande subcategorias. */
import $ from 'jquery';
import { esc, fmt, valueColor, monthLabel } from '@/core/kernel/_0_domain/format.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import type { CacheStore } from '@/core/kernel/_1_application/cache-store.ts';
import { icon } from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import { createPage } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '@/core/kernel/_2_infrastructure/primary/page.ts';
import { emptyState } from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { pageHeader } from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import { btn } from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import { toast } from '@/core/kernel/_2_infrastructure/primary/ui/toast.ts';
import type { CategoryEvolutionService } from './service.ts';
import * as Domain from './domain.ts';

export interface CategoryEvolutionPageDeps {
  service: CategoryEvolutionService;
  cache: CacheStore;
}

interface CategoryEvolutionPageState extends PageState {
  interval: Domain.Interval;
  startMonth: number;
  startYear: number;
  size: number;
  loading: boolean;
  matrix: Domain.EvolutionMatrix | null;
  expanded: Record<string, boolean>;
  error: string | null;
}

export function createCategoryEvolutionPage(deps: CategoryEvolutionPageDeps): Page {
  let state: CategoryEvolutionPageState | null = null;

  function loadEvolution(): Promise<void> {
    if (!state) return Promise.resolve();
    state.loading = true;
    state.error = null;
    render();

    const start = Period.create(state.startMonth + 1, state.startYear);
    return deps.service
      .load(state.interval, start, state.size)
      .then((matrix) => {
        if (!state) return;
        state.matrix = matrix;
        state.loading = false;
        render();
      })
      .catch((err: { message?: string }) => {
        if (!state) return;
        state.loading = false;
        state.matrix = null;
        state.error = (err && err.message) || 'Falha ao carregar relatório';
        render();
        toast(state.error, 'error');
      });
  }

  function isVisible(row: Domain.EvolutionRow): boolean {
    if (!state) return false;
    if (row.level === 0) return true; // Grupos de natureza sempre visíveis
    if (row.level === 1) {
      // Macro: visível se seu grupo está expandido
      return (state.expanded['group:' + row.nature] ?? false) === true;
    }
    // Sub: visível se grupo e macro estão expandidos
    const groupKey = 'group:' + row.nature;
    const groupExp = (state.expanded[groupKey] ?? false) === true;
    const macroExp = row.parentId ? (state.expanded[row.parentId] ?? false) === true : false;
    return groupExp && macroExp;
  }

  function zeroCell(): JQuery {
    return $('<td style="text-align:center;color:var(--text-muted);">-</td>');
  }

  function renderTable(): JQuery {
    if (!state || !state.matrix) return $('<div></div>');

    const { buckets, rows } = state.matrix;
    const incomeByBucket = state!.matrix!.incomeByBucket;

    const $table = $('<table class="report-table"></table>');

    // thead
    const $thead = $('<thead></thead>');
    const $headRow = $('<tr></tr>');
    $headRow.append('<th style="text-align:left;min-width:240px;"></th>');
    buckets.forEach((b) => {
      $headRow.append('<th>' + esc(b.label) + '</th>');
    });
    $headRow.append('<th>Média</th><th>Total</th>');
    $thead.append($headRow);
    $table.append($thead);

    // tbody
    const $tbody = $('<tbody></tbody>');
    rows.forEach((row) => {
      if (!isVisible(row)) return;

      const $tr = $('<tr' + (row.level === 0 ? ' class="report-row-group report-row-group--' + row.nature.toLowerCase() + '"' : '') + '></tr>');

      // Label + toggle
      const padding = [12, 28, 48][row.level];
      let labelCell = '<td style="padding-left:' + padding + 'px;">';
      if (row.hasChildren) {
        const isExpanded = state!.expanded[row.id];
        labelCell += '<button class="report-toggle" data-role="toggle" data-id="' + esc(row.id) + '" style="cursor:pointer;border:none;background:none;color:inherit;padding:0;font-size:inherit;">' +
          icon(isExpanded ? 'chevronDown' : 'chevronRight', 13) +
          ' ' + esc(row.label) +
        '</button>';
      } else {
        labelCell += esc(row.label);
      }
      labelCell += '</td>';
      $tr.append(labelCell);

      // Células de valor por bucket
      row.values.forEach((value, idx) => {
        if (value === 0) {
          $tr.append(zeroCell());
          return;
        }
        const $cell = $('<td></td>');
        const share = Domain.shareOf(value, incomeByBucket[idx] ?? 0);
        const shareHtml = row.nature === 'EXPENSE' && share != null
          ? '<span class="report-cell-share">' + esc(share.toFixed(2) + '%') + '</span>'
          : '<span class="report-cell-share">-</span>';
        $cell.html(
          '<div style="color:' + valueColor(value) + ';">' + esc(fmt(value)) + '</div>' + shareHtml
        );
        $tr.append($cell);
      });

      // Média e Total
      if (row.average === 0) {
        $tr.append(zeroCell());
      } else {
        $tr.append('<td><div style="color:' + valueColor(row.average) + ';">' + esc(fmt(row.average)) + '</div></td>');
      }

      if (row.total === 0) {
        $tr.append(zeroCell());
      } else {
        $tr.append('<td><div style="color:' + valueColor(row.total) + ';">' + esc(fmt(row.total)) + '</div></td>');
      }

      $tbody.append($tr);
    });
    $table.append($tbody);

    // tfoot
    const $tfoot = $('<tfoot></tfoot>');
    const $footRow = $('<tr></tr>');
    $footRow.append('<td style="padding-left:12px;"><strong>Resultado</strong></td>');

    const resultTotal = state.matrix.resultByBucket.reduce((a, b) => a + b, 0);
    const resultAvg = resultTotal / buckets.length;

    state.matrix.resultByBucket.forEach((result) => {
      if (result === 0) {
        $footRow.append(zeroCell());
        return;
      }
      $footRow.append('<td><div style="color:' + valueColor(result) + ';">' + esc(fmt(result)) + '</div></td>');
    });

    if (resultAvg === 0) {
      $footRow.append(zeroCell());
    } else {
      $footRow.append('<td><div style="color:' + valueColor(resultAvg) + ';">' + esc(fmt(resultAvg)) + '</div></td>');
    }

    if (resultTotal === 0) {
      $footRow.append(zeroCell());
    } else {
      $footRow.append('<td><div style="color:' + valueColor(resultTotal) + ';">' + esc(fmt(resultTotal)) + '</div></td>');
    }

    $tfoot.append($footRow);
    $table.append($tfoot);

    return $('<div class="report-table-wrap"></div>').append($table);
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;
    $root.empty();

    const $header = pageHeader({ title: 'Evolução por categoria' });
    $root.append($header);

    // Filtros
    const $filters = $('<div class="card" style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;"></div>');

    const $intervalSel = $(
      '<select data-role="interval" style="flex:none;width:120px;">' +
        '<option value="month"' + (state.interval === 'month' ? ' selected' : '') + '>Mensal</option>' +
        '<option value="year"' + (state.interval === 'year' ? ' selected' : '') + '>Anual</option>' +
      '</select>'
    );
    $filters.append($intervalSel);

    if (state!.interval === 'month') {
      const $monthSel = $(
        '<select data-role="start-month" style="flex:none;width:90px;">' +
          Array.from({ length: 12 }, (_, i) => {
            const mon = i + 1;
            return '<option value="' + i + '"' + (mon === state!.startMonth + 1 ? ' selected' : '') + '>' +
              monthLabel(mon, state!.startYear).substring(0, 3) +
            '</option>';
          }).join('') +
        '</select>'
      );
      $filters.append($monthSel);
    }

    const $yearInp = $('<input type="number" data-role="start-year" min="2000" max="2099" value="' + state!.startYear + '" style="flex:none;width:80px;" />');
    $filters.append($yearInp);

    const sizes = state!.interval === 'month' ? ['6', '12', '24', '36'] : ['3', '5', '10'];
    const $sizeSel = $('<select data-role="size" style="flex:none;width:70px;">' + sizes.map((s) => '<option value="' + s + '"' + (parseInt(s) === state!.size ? ' selected' : '') + '>' + s + '</option>').join('') + '</select>');
    $filters.append($sizeSel);

    $filters.append(btn({ label: 'Carregar', variant: 'primary', attrs: 'data-role="load"' }));

    $root.append($filters);

    // Conteúdo
    if (state!.loading) {
      $root.append('<div style="text-align:center;padding:40px;"><div style="font-size:14px;color:var(--text-muted);">Carregando…</div></div>');
    } else if (!state!.matrix) {
      $root.append(emptyState({ title: 'Nenhum relatório carregado', desc: 'Clique em "Carregar" para exibir os dados' }));
    } else {
      $root.append(renderTable());
    }
  }

  function bind($root: JQuery): void {
    $root.off('.report-cat-evolution');

    $root.on('change.report-cat-evolution', '[data-role=interval]', function () {
      if (!state) return;
      state.interval = $(this).val() as Domain.Interval;
      render();
    });

    $root.on('click.report-cat-evolution', '[data-role=load]', function () {
      if (!state) return;
      state.startMonth = parseInt($root.find('[data-role=start-month]').val() as string) ?? state.startMonth;
      state.startYear = parseInt($root.find('[data-role=start-year]').val() as string) ?? state.startYear;
      state.size = parseInt($root.find('[data-role=size]').val() as string) ?? state.size;
      loadEvolution();
    });

    $root.on('click.report-cat-evolution', '[data-role=toggle]', function () {
      if (!state) return;
      const id = $(this).data('id');
      state.expanded[id] = !state.expanded[id];
      render();
    });
  }

  function onMount(): void {
    if (!state) return;
    loadEvolution();
  }

  return createPage<CategoryEvolutionPageState>({
    ns: '.report-cat-evolution',
    state: () => {
      const start = Period.shift(Period.currentMonth(), -11);
      state = {
        interval: 'month',
        startMonth: start.month - 1,
        startYear: start.year,
        size: 12,
        loading: false,
        matrix: null,
        expanded: { 'group:INCOME': true, 'group:EXPENSE': true },
        error: null,
      };
      return state;
    },
    render,
    bind,
    onMount,
  });
}

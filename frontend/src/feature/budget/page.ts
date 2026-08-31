/** pages/budget — Metas / Orçamento (lista de metas + CRUD modal). */
import $ from 'jquery';
import {esc, fmt, maskCurrency, parseCurrency} from '@/core/kernel/_0_domain/format.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import type {CacheStore} from '@/core/kernel/_1_application/cache-store.ts';
import {
    bindCurrencyMask,
    bindRecordActions,
    byId,
    confirmModal,
    formModal,
    modalText,
    periodNavFor,
    runMutation
} from '@/core/kernel/_2_infrastructure/primary/helpers.ts';
import {icon, ICONS} from '@/core/kernel/_2_infrastructure/primary/icons.ts';
import type {Page, PageState} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {createPage} from '@/core/kernel/_2_infrastructure/primary/page.ts';
import {categoryPickerHtml, PALETTE} from '@/core/kernel/_2_infrastructure/primary/pickers.ts';
import {badge} from '@/core/kernel/_2_infrastructure/primary/ui/badge.ts';
import {statCardHtml} from '@/core/kernel/_2_infrastructure/primary/ui/card.ts';
import {emptyState} from '@/core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import {pageHeader} from '@/core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import {progressBarHtml} from '@/core/kernel/_2_infrastructure/primary/ui/progress-bar.ts';
import {btn, rowActionBtn} from '@/core/kernel/_2_infrastructure/primary/ui/button.ts';
import {toast} from '@/core/kernel/_2_infrastructure/primary/ui/toast.ts';
import * as BudgetDomain from '@/feature/budget/domain.ts';
import type {BudgetService} from '@/feature/budget/service.ts';
import type {BudgetResponse} from '@/feature/budget/types.ts';

const ICON_CHOICES = [
  'list', 'tag', 'creditCard', 'building', 'briefcase',
  'home', 'target', 'pieChart', 'activity', 'barChart',
  'calendar', 'trendingUp', 'trendingDown', 'dollarSign', 'hash',
];

export interface BudgetPageDeps {
  service: BudgetService;
  periodService: { get(): Period.Period; set(month: number, year: number): unknown };
  cache: CacheStore;
}

interface BudgetPageState extends PageState {
  loading: boolean;
  items: BudgetResponse[];
  month: number;
  year: number;
}

export function createBudgetPage(deps: BudgetPageDeps): Page {
  let state: BudgetPageState | null = null;

  function findById(id: string): BudgetResponse | null {
    return state ? byId(state.items, id) : null;
  }

  function categoryName(id: string | null | undefined): string | null {
    const c = deps.cache.findById('categories', id);
    return c ? c.name : null;
  }

  function colorFor(item: BudgetResponse, idx: number): string {
    return item.color || PALETTE.swatches[idx % PALETTE.swatches.length] || PALETTE.swatches[0];
  }

  // Backend pode mandar um glifo (emoji) ou nome de ícone. Sempre renderizamos um SVG inline; se
  // o valor bate com uma chave conhecida de ICONS, usa, senão cai pra 'target'.
  function iconFor(item: BudgetResponse | null | undefined): string {
    const name = item && item.icon;
    return name && ICONS[name] ? name : 'target';
  }

  function loadBudget(): Promise<void> {
    if (!state) return Promise.resolve();
    state.loading = true;
    render();
    const period = Period.create(state.month + 1, state.year);
    return deps.service
      .loadPeriod(period)
      .then((list) => {
        if (!state) return;
        state.items = Array.isArray(list) ? list.slice() : [];
        state.loading = false;
        render();
      })
      .catch((err: { message?: string }) => {
        if (!state) return;
        state.loading = false;
        state.items = [];
        render();
        toast((err && err.message) || 'Falha ao carregar metas', 'error');
      });
  }

  function renderRow(item: BudgetResponse, idx: number): JQuery {
    const name = item.name || categoryName(item.categoryId) || 'Meta sem categoria';
    const budgeted = +(item.budgeted || 0);
    const spent = +(item.spent || 0);
    const pctRaw = budgeted > 0 ? (spent / budgeted) * 100 : 0;
    const pct = BudgetDomain.consumptionPct(spent, budgeted);
    const over = BudgetDomain.isOverBudget(spent, budgeted);
    const barColorToken = 'var(--' + BudgetDomain.barColor(pctRaw) + ')';
    const color = colorFor(item, idx);
    const icName = iconFor(item);
    const diff = budgeted - spent;

    const $card = $('<div class="card" data-id="' + esc(item.id) + '" style="padding:16px 20px;display:flex;flex-direction:column;gap:10px;"></div>');

    const headHtml =
      '<div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">' +
        '<div style="display:flex;align-items:center;gap:10px;min-width:0;">' +
          '<span style="width:32px;height:32px;border-radius:8px;background:' + esc(color) + '22;' +
            'color:' + esc(color) + ';display:inline-flex;align-items:center;justify-content:center;flex-shrink:0;">' +
            icon(icName, 16) +
          '</span>' +
          '<span style="font-size:14px;font-weight:700;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' +
            esc(name) +
          '</span>' +
          (over ? ' ' + badge('Estourado', 'expense') : '') +
        '</div>' +
        '<div style="display:flex;align-items:center;gap:14px;">' +
          '<div style="text-align:right;display:flex;gap:6px;align-items:baseline;">' +
            '<span style="font-size:13px;font-weight:700;color:' + (over ? 'var(--expense)' : 'var(--text-primary)') + ';">' +
              esc(fmt(spent)) +
            '</span>' +
            '<span style="font-size:12px;color:var(--text-secondary);font-weight:600;">' +
              '/ ' + esc(fmt(budgeted)) +
            '</span>' +
          '</div>' +
          '<div data-region="row-actions" style="display:flex;gap:2px;"></div>' +
        '</div>' +
      '</div>';
    $card.append(headHtml);

    $card.append(progressBarHtml(pct, barColorToken));

    const footerHtml =
      '<div style="display:flex;justify-content:space-between;font-size:11px;">' +
        '<span style="color:var(--text-muted);">' + (budgeted > 0 ? pctRaw.toFixed(0) + '% utilizado' : 'Sem orçamento') + '</span>' +
        '<span style="font-weight:600;color:' + (over ? 'var(--expense)' : 'var(--income)') + ';">' +
          (over ? esc(fmt(diff)) + ' acima' : esc(fmt(Math.max(0, diff))) + ' restante') +
        '</span>' +
      '</div>';
    $card.append(footerHtml);

    const $actions = $card.find('[data-region=row-actions]');
    $actions.append(rowActionBtn('edit', 'Editar', item.id));
    $actions.append(rowActionBtn('trash', 'Excluir', item.id, true));

    return $card;
  }

  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;
    $root.empty();

    const $header = pageHeader({
      title: 'Metas / Orçamento',
      actions: btn({ variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Meta', attrs: 'data-act="new"' }),
      nav: periodNavFor(state, { oneBased: false, periodService: deps.periodService, onChange: loadBudget }),
    });
    $root.append($header);

    let totalBudgeted = 0;
    let totalSpent = 0;
    state.items.forEach((it) => {
      totalBudgeted += +(it.budgeted || 0);
      totalSpent += +(it.spent || 0);
    });
    const normalized = state.items.map((it) => BudgetDomain.normalize(it)).filter((it): it is NonNullable<typeof it> => it != null);
    const overCount = BudgetDomain.overspendCount(normalized);
    const pctTotal = BudgetDomain.consumptionPct(totalSpent, totalBudgeted);

    const summaryHtml =
      '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-bottom:20px;">' +
        statCardHtml({ label: 'Orçamento Total', value: fmt(totalBudgeted), color: 'var(--text-primary)' }) +
        statCardHtml({
          label: 'Total Gasto', value: fmt(totalSpent),
          color: totalSpent > totalBudgeted && totalBudgeted > 0 ? 'var(--expense)' : 'var(--text-primary)',
          sub: totalBudgeted > 0 ? pctTotal.toFixed(0) + '% do orçamento' : '—',
        }) +
        statCardHtml({
          label: 'Categorias Estouradas', value: String(overCount),
          color: overCount > 0 ? 'var(--expense)' : 'var(--income)',
        }) +
      '</div>';
    $root.append(summaryHtml);

    if (state.loading) {
      $root.append(emptyState({ icon: 'target', title: 'Carregando…' }));
      return;
    }
    if (!state.items.length) {
      $root.append(
        emptyState({
          icon: 'target',
          title: 'Nenhuma meta cadastrada',
          desc: 'Clique em “Nova Meta” para configurar o orçamento deste mês.',
        }),
      );
      return;
    }

    const $list = $('<div style="display:flex;flex-direction:column;gap:12px;"></div>');
    state.items.forEach((it, idx) => {
      $list.append(renderRow(it, idx));
    });
    $root.append($list);
  }

  function openFormModal(existing: BudgetResponse | null): void {
    const isEdit = !!existing;
    const initBudgeted = isEdit && existing ? +(existing.budgeted || 0) : 0;
    const initCategoryId = isEdit && existing ? existing.categoryId || '' : '';
    const initColor = isEdit && existing ? existing.color || PALETTE.swatches[0] : PALETTE.swatches[0];
    const initIcon = isEdit ? iconFor(existing) : 'target';

    const uid = 'bd-' + Date.now();
    const amountInitial = initBudgeted > 0 ? maskCurrency(initBudgeted) : '';

    // keepId: em edição, categoria já gravada continua na lista mesmo se foi inativada depois — o
    // campo fica disabled (categoria não muda pós-criação), mas não pode sumir da tela.
    const categories = deps.cache.categories();
    const categoryFieldHtml = categoryPickerHtml({
      items: categories.filter((c) => c.nature === 'EXPENSE').map((c) => ({ id: c.id, label: c.name })),
      selectedId: initCategoryId,
      selectId: uid + '-cat',
      selectAttrs: ' name="categoryId"' + (isEdit ? ' disabled' : ''),
      placeholder: '— Selecione —',
      disabled: isEdit,
    });

    const colorSwatches = PALETTE.swatches
      .map((c) => {
        const active = c === initColor;
        return '<button type="button" class="swatch swatch-lg' + (active ? ' is-active' : '') + '" ' + 'data-color-pick="' + esc(c) + '" style="background:' + esc(c) + ';"></button>';
      })
      .join('');

    const iconChoices = ICON_CHOICES.map((n) => {
      const active = n === initIcon;
      return '<button type="button" class="icon-btn icon-pick' + (active ? ' is-active' : '') + '" ' + 'data-icon-pick="' + esc(n) + '">' + icon(n, 16) + '</button>';
    }).join('');

    const bodyHtml =
      '<form data-form="bd" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + uid + '-cat">Categoria</label>' +
            categoryFieldHtml +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + uid + '-amt">Valor orçado (R$)</label>' +
            '<input id="' + uid + '-amt" name="budgeted" type="text" inputmode="numeric" ' +
              'placeholder="0,00" value="' + esc(amountInitial) + '" required />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label">Cor</label>' +
            '<div data-region="colors" style="display:flex;flex-wrap:wrap;gap:8px;margin-top:4px;">' +
              colorSwatches +
            '</div>' +
            '<input type="hidden" name="color" value="' + esc(initColor) + '" />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label">Ícone</label>' +
            '<div data-region="icons" style="display:flex;flex-wrap:wrap;gap:6px;margin-top:4px;">' +
              iconChoices +
            '</div>' +
            '<input type="hidden" name="icon" value="' + esc(initIcon) + '" />' +
          '</div>' +
        '</div>' +
      '</form>';

    const m = formModal({
      title: isEdit ? 'Editar Meta' : 'Nova Meta',
      formName: 'bd',
      body: bodyHtml,
      onSubmit: ($form) => {
        const categoryId = ($form.find('select[name=categoryId]').val() as string) || '';
        const amountRaw = ($form.find('input[name=budgeted]').val() as string) || '';
        const color = ($form.find('input[name=color]').val() as string) || PALETTE.swatches[0];
        const iconName = ($form.find('input[name=icon]').val() as string) || 'target';
        const budgeted = parseCurrency(amountRaw);

        if (!isEdit && !categoryId) {
          toast('Selecione uma categoria', 'error');
          $form.find('select[name=categoryId]').trigger('focus');
          return null;
        }
        if (!isFinite(budgeted) || budgeted <= 0) {
          toast('Informe um valor válido', 'error');
          $form.find('input[name=budgeted]').trigger('focus');
          return null;
        }

        if (isEdit && existing) return deps.service.save(existing.id, { budgeted, color, icon: iconName });
        if (!state) return null;
        return deps.service.create({ budgeted, color, icon: iconName, categoryId, month: state.month + 1, year: state.year });
      },
      success: () => (isEdit ? 'Meta atualizada' : 'Meta criada'),
      failure: 'Falha ao salvar meta',
      onDone: loadBudget,
    });

    bindCurrencyMask(m.$body.find('input[name=budgeted]'));

    m.$body.on('click', '[data-color-pick]', function () {
      const c = $(this).attr('data-color-pick');
      m.$body.find('input[name=color]').val(c || '');
      m.$body.find('[data-color-pick]').each(function () {
        $(this).toggleClass('is-active', $(this).attr('data-color-pick') === c);
      });
    });

    m.$body.on('click', '[data-icon-pick]', function () {
      const name = $(this).attr('data-icon-pick');
      m.$body.find('input[name=icon]').val(name || '');
      m.$body.find('[data-icon-pick]').each(function () {
        $(this).toggleClass('is-active', $(this).attr('data-icon-pick') === name);
      });
    });
  }

  function openDeleteModal(target: BudgetResponse): void {
    const name = target.name || categoryName(target.categoryId) || 'esta meta';
    confirmModal({
      title: 'Excluir Meta',
      body: modalText('Tem certeza que deseja excluir a meta <strong>' + esc(name) + '</strong>? Esta ação não pode ser desfeita.'),
      onConfirm: (m, reEnable) => {
        runMutation(deps.service.remove(target.id), {
          modal: m, success: 'Meta excluída', failure: 'Falha ao excluir meta',
          onDone: loadBudget, onError: reEnable,
        });
      },
    });
  }

  function bindRoot($root: JQuery): void {
    bindRecordActions($root, '.bd', { find: findById, onNew: () => openFormModal(null), onEdit: openFormModal, onDelete: openDeleteModal });
  }

  return createPage<BudgetPageState>({
    ns: '.bd',
    state: () => {
      const p = deps.periodService.get();
      state = { loading: true, items: [], month: p.month - 1, year: p.year };
      return state;
    },
    render,
    bind: bindRoot,
    onMount: () => {
      loadBudget();
    },
  });
}

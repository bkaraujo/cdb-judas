/** pages/dashboard — Visão Geral (painéis configuráveis + drag-reorder + modal personalizar).
 * Última fatia: lê de quase todas as outras via seus `api.ts` (accounts-payable, credit-cards,
 * budget, accounts, categories) — nenhuma domain própria de payable/budget/cartão. */
import $ from 'jquery';
import { esc, fmt } from '../../core/kernel/_0_domain/format.ts';
import * as Period from '../../core/kernel/_0_domain/period.ts';
import type { CacheStore } from '../../core/kernel/_1_application/cache-store.ts';
import type { PreferencesService } from '../../core/kernel/_1_application/preferences-service.ts';
import { createPage } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import type { Page, PageState } from '../../core/kernel/_2_infrastructure/primary/page.ts';
import { icon } from '../../core/kernel/_2_infrastructure/primary/icons.ts';
import { PALETTE } from '../../core/kernel/_2_infrastructure/primary/pickers.ts';
import { btn } from '../../core/kernel/_2_infrastructure/primary/ui/button.ts';
import { emptyState } from '../../core/kernel/_2_infrastructure/primary/ui/empty-state.ts';
import { modal } from '../../core/kernel/_2_infrastructure/primary/ui/modal.ts';
import { pageHeader } from '../../core/kernel/_2_infrastructure/primary/ui/page-header.ts';
import { progressBarHtml } from '../../core/kernel/_2_infrastructure/primary/ui/progress-bar.ts';
import type { AccountsPayableApi } from '../accounts-payable/api.ts';
import type { BudgetApi } from '../budget/api.ts';
import type { AccountsApi } from '../accounts/api.ts';
import type { CategoriesApi } from '../categories/api.ts';
import type { CreditCardsApi } from '../credit-cards/api.ts';
import type { TransactionsApi } from '../transactions/api.ts';
import * as DashboardAggregations from './domain.ts';
import { PANELS } from './panels/index.ts';
import type { CategoryBarDatum, DashboardData, LabeledBucket, PanelCtx, PanelDef, PayableListItem } from './panels/types.ts';

const SERIES_PALETTE = PALETTE.series;

// ── Definição dos painéis (port direto de dashboard.jsx) ──────
const ALL_PANELS: PanelDef[] = [
  { id: 'cash-balances', label: 'Saldos de caixa', defaultOn: true, icon: 'building' },
  { id: 'month-result', label: 'Resultado do mês', defaultOn: true, icon: 'activity' },
  { id: 'expenses-cat', label: 'Despesas por categoria', defaultOn: true, icon: 'pieChart' },
  { id: 'accounts-payable', label: 'Contas a pagar', defaultOn: true, icon: 'calendar' },
  { id: 'accounts-receivable', label: 'Contas a receber', defaultOn: false, icon: 'arrowUp' },
  { id: 'credit-cards', label: 'Cartões de crédito', defaultOn: true, icon: 'creditCard' },
  { id: 'recent-postings', label: 'Últimos lançamentos', defaultOn: true, icon: 'list' },
];

interface DashboardSettings {
  viewMode: 'cash' | 'accrual' | 'define';
  columns: 1 | 2 | 3;
  scrollPanels: boolean;
  includeInvestments: boolean;
  enabled: Record<string, boolean>;
  panelOrder: string[];
}

const DEFAULT_ENABLED: Record<string, boolean> = {};
ALL_PANELS.forEach((p) => {
  DEFAULT_ENABLED[p.id] = p.defaultOn;
});

const DEFAULT_SETTINGS: DashboardSettings = {
  viewMode: 'cash',
  columns: 2,
  scrollPanels: false,
  includeInvestments: true,
  enabled: DEFAULT_ENABLED,
  panelOrder: ALL_PANELS.map((p) => p.id),
};

function cloneSettings(s: DashboardSettings): DashboardSettings {
  return JSON.parse(JSON.stringify(s)) as DashboardSettings;
}

function isDashboardSettings(v: unknown): v is Partial<DashboardSettings> {
  return !!v && typeof v === 'object';
}

export interface DashboardPageDeps {
  cache: CacheStore;
  preferences: PreferencesService;
  transactions: TransactionsApi;
  accountsPayable: AccountsPayableApi;
  creditCards: CreditCardsApi;
  budget: BudgetApi;
  accounts: AccountsApi;
  categories: CategoriesApi;
}

interface DashboardPageState extends PageState {
  settings: DashboardSettings;
  hideValues: boolean;
  draggedId: string | null;
  dragOverId: string | null;
  data: DashboardData;
  unsubscribeAcc?: () => void;
  unsubscribeCat?: () => void;
}

export function createDashboardPage(deps: DashboardPageDeps): Page {
  let state: DashboardPageState | null = null;

  function loadSettings(): DashboardSettings {
    const parsed = deps.preferences.getDashboardSettings();
    if (!isDashboardSettings(parsed)) return cloneSettings(DEFAULT_SETTINGS);
    const merged: DashboardSettings = { ...DEFAULT_SETTINGS, ...parsed, enabled: { ...DEFAULT_SETTINGS.enabled, ...(parsed.enabled || {}) } };
    const order = merged.panelOrder && merged.panelOrder.length ? merged.panelOrder.slice() : [];
    ALL_PANELS.forEach((p) => {
      if (!(p.id in merged.enabled)) merged.enabled[p.id] = p.defaultOn;
      if (order.indexOf(p.id) === -1) order.push(p.id);
    });
    merged.panelOrder = order;
    return merged;
  }

  function saveSettings(s: DashboardSettings): void {
    deps.preferences.saveDashboardSettings(s);
  }

  function pickColor(i: number, fallback?: string | null): string {
    return fallback || SERIES_PALETTE[i % SERIES_PALETTE.length] || SERIES_PALETTE[0] || '';
  }

  // ── smoothPath (Catmull-Rom-ish) — porta de theme.jsx ────────
  interface Pt {
    x: number;
    y: number;
  }
  function smoothPath(pts: readonly Pt[]): string {
    if (!pts || pts.length < 2) return '';
    const p0first = pts[0] as Pt;
    let d = 'M ' + p0first.x + ',' + p0first.y;
    for (let i = 1; i < pts.length; i++) {
      const p0 = pts[Math.max(0, i - 2)] as Pt;
      const p1 = pts[i - 1] as Pt;
      const p2 = pts[i] as Pt;
      const p3 = pts[Math.min(pts.length - 1, i + 1)] as Pt;
      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;
      d += ' C ' + cp1x + ',' + cp1y + ' ' + cp2x + ',' + cp2y + ' ' + p2.x + ',' + p2.y;
    }
    return d;
  }

  function cbdAccounts() {
    return deps.cache.accounts();
  }

  function v(n: number): string {
    if (state?.hideValues) return 'R$ •••••';
    return fmt(n);
  }

  function loadAll(): Promise<void> {
    if (!state) return Promise.resolve();
    const jobs: [ReturnType<TransactionsApi['list']>, ReturnType<AccountsPayableApi['listPayable']>, ReturnType<AccountsPayableApi['listReceivable']>] = [
      deps.transactions.list('limit=500&sort=date,desc').catch(() => []),
      deps.accountsPayable.listPayable().catch(() => []),
      deps.accountsPayable.listReceivable().catch(() => []),
    ];
    return Promise.all(jobs).then(([transactions, payables, receivables]) => {
      if (!state) return;
      state.data.transactions = Array.isArray(transactions) ? transactions : [];
      state.data.payables = Array.isArray(payables) ? payables : [];
      state.data.receivables = Array.isArray(receivables) ? receivables : [];
      state.data.loaded = true;
    });
  }

  function cashAccounts() {
    return cbdAccounts();
  }
  function creditCardAccounts() {
    return deps.creditCards.accountsWithCards();
  }

  function currentMonthTxs() {
    const p = Period.currentMonth();
    return (state?.data.transactions || []).filter((t) => t.date && Period.containsDate(p, t.date));
  }

  function categoryName(catId: string | null | undefined): string {
    return DashboardAggregations.categoryNameFor({ categoryId: catId }, deps.cache.categories());
  }

  function txIsExpense(t: { type?: string | null; amount?: number | string | null; categoryId?: string | null }): boolean {
    const nat = DashboardAggregations.categoryNatureFor(t, deps.cache.categories());
    return DashboardAggregations.txIsExpense(t, nat);
  }

  function expenseByCategory(): CategoryBarDatum[] {
    const list = DashboardAggregations.expenseByCategory(currentMonthTxs(), deps.cache.categories());
    return list.map((e, i) => ({ name: e.name, amount: e.value, color: SERIES_PALETTE[i % SERIES_PALETTE.length] || SERIES_PALETTE[0] || '' }));
  }

  function monthShortLabel(d: Date): string {
    const s = new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(d).replace('.', '');
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  function monthlySeries(nMonths: number): LabeledBucket[] {
    const pts = DashboardAggregations.monthlySeries(state?.data.transactions || [], nMonths);
    return pts.map((p) => {
      const d = new Date(p.year, p.month - 1, 1);
      return { ...p, month: monthShortLabel(d) };
    });
  }

  function upcomingPayables(typ: 'expense' | 'income'): PayableListItem[] {
    const src = typ === 'expense' ? state?.data.payables : state?.data.receivables;
    return DashboardAggregations.upcomingPayables(src || [], deps.accountsPayable.isActive);
  }

  // ── Charts ───────────────────────────────────────────────────
  function miniLineChart(data: readonly LabeledBucket[], height?: number): string {
    const H = height || 100;
    const W = 400;
    const pad = { l: 10, r: 10, t: 8, b: 20 };
    const cw = W - pad.l - pad.r;
    const ch = H - pad.t - pad.b;
    const flat: number[] = [];
    data.forEach((d) => {
      flat.push(d.receitas || 0);
      flat.push(d.despesas || 0);
    });
    let maxV = Math.max(...flat) * 1.1;
    if (!maxV || !isFinite(maxV)) maxV = 1;
    function toX(i: number): number {
      return pad.l + (i / Math.max(1, data.length - 1)) * cw;
    }
    function toY(val: number): number {
      return pad.t + (1 - val / maxV) * ch;
    }
    const incPts = data.map((d, i) => ({ x: toX(i), y: toY(d.receitas || 0) }));
    const expPts = data.map((d, i) => ({ x: toX(i), y: toY(d.despesas || 0) }));
    const lastX = toX(data.length - 1);
    const areaInc = smoothPath(incPts) + ' L ' + lastX + ',' + (H - pad.b) + ' L ' + pad.l + ',' + (H - pad.b) + ' Z';
    const areaExp = smoothPath(expPts) + ' L ' + lastX + ',' + (H - pad.b) + ' L ' + pad.l + ',' + (H - pad.b) + ' Z';

    const defs =
      '<defs>' +
        '<linearGradient id="mg-inc" x1="0" y1="0" x2="0" y2="1">' +
          '<stop offset="0%" stop-color="var(--income)" stop-opacity="0.2"/>' +
          '<stop offset="100%" stop-color="var(--income)" stop-opacity="0.01"/>' +
        '</linearGradient>' +
        '<linearGradient id="mg-exp" x1="0" y1="0" x2="0" y2="1">' +
          '<stop offset="0%" stop-color="var(--expense)" stop-opacity="0.15"/>' +
          '<stop offset="100%" stop-color="var(--expense)" stop-opacity="0.01"/>' +
        '</linearGradient>' +
      '</defs>';

    const labels = data
      .map((d, i) => '<text x="' + toX(i) + '" y="' + (H - 6) + '" text-anchor="middle" font-size="9" fill="var(--text-muted)">' + esc(d.month) + '</text>')
      .join('');

    function circles(pts: readonly Pt[], color: string): string {
      return pts.map((p) => '<circle cx="' + p.x + '" cy="' + p.y + '" r="3" fill="' + color + '" stroke="var(--bg-card)" stroke-width="1.5"/>').join('');
    }

    return (
      '<svg viewBox="0 0 ' + W + ' ' + H + '" style="width:100%;height:100%;display:block;">' +
        defs +
        labels +
        '<path d="' + areaInc + '" fill="url(#mg-inc)"/>' +
        '<path d="' + areaExp + '" fill="url(#mg-exp)"/>' +
        '<path d="' + smoothPath(incPts) + '" fill="none" stroke="var(--income)" stroke-width="2" stroke-linecap="round"/>' +
        '<path d="' + smoothPath(expPts) + '" fill="none" stroke="var(--expense)" stroke-width="2" stroke-linecap="round"/>' +
        circles(incPts, 'var(--income)') +
        circles(expPts, 'var(--expense)') +
      '</svg>'
    );
  }

  function categoryBars(data: readonly CategoryBarDatum[]): string {
    if (!data.length) {
      return emptyState({ icon: 'pieChart', title: 'Sem dados', desc: 'Nenhum lançamento no período.' });
    }
    let max = Math.max(...data.map((d) => d.amount));
    if (!max) max = 1;
    let html = '<div style="display:flex;flex-direction:column;gap:8px;">';
    data.forEach((d) => {
      const pct = (d.amount / max) * 100;
      html +=
        '<div>' +
          '<div style="display:flex;justify-content:space-between;margin-bottom:4px;">' +
            '<span style="font-size:12px;color:var(--text-secondary);">' + esc(d.name) + '</span>' +
            '<span style="font-size:12px;font-weight:700;color:' + esc(d.color) + ';">' + esc(v(d.amount)) + '</span>' +
          '</div>' +
          progressBarHtml(pct, esc(d.color), { size: 'sm' }) +
        '</div>';
    });
    html += '</div>';
    return html;
  }

  // ── Panel skeleton (PanelWrap) ───────────────────────────────
  function panelWrap(opts: { title: string; icon?: string; action?: JQuery | string; body: JQuery | string }): JQuery {
    const $card = $('<div class="card panel"></div>');
    const $head = $('<div class="panel-head" style="flex-shrink:0;"></div>');

    const gripSvg =
      '<svg width="10" height="14" viewBox="0 0 10 14" fill="var(--text-muted)" style="flex-shrink:0;opacity:0.5;">' +
        '<circle cx="2" cy="2" r="1.3"/><circle cx="2" cy="6" r="1.3"/><circle cx="2" cy="11" r="1.3"/>' +
        '<circle cx="6" cy="2" r="1.3"/><circle cx="6" cy="6" r="1.3"/><circle cx="6" cy="11" r="1.3"/>' +
      '</svg>';

    const $left = $(
      '<div style="display:flex;align-items:center;gap:8px;min-width:0;">' +
        gripSvg +
        '<span style="display:flex;align-items:center;color:var(--text-muted);flex-shrink:0;">' + icon(opts.icon || 'activity', 14) + '</span>' +
        '<span style="font-size:13px;font-weight:700;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' + esc(opts.title) + '</span>' +
      '</div>',
    );
    $head.append($left);

    const $right = $('<div style="display:flex;align-items:center;gap:6px;flex-shrink:0;"></div>');
    if (opts.action) $right.append(opts.action);
    $head.append($right);

    $card.append($head);

    const $body = $('<div class="panel-body' + (state?.settings.scrollPanels ? ' panel-body--scroll' : '') + '"></div>');
    if (opts.body) {
      if (typeof opts.body === 'string') $body.html(opts.body);
      else $body.append(opts.body);
    }
    $card.append($body);

    return $card;
  }

  function stubPanel(p: PanelDef): JQuery {
    return panelWrap({
      title: p.label,
      icon: p.icon || 'barChart',
      body: emptyState({ icon: 'activity', title: 'Em breve', desc: 'Dados disponíveis em breve.' }),
    });
  }

  // ── Panel context (shared utilities for external panel files) ─
  function buildCtx(): PanelCtx {
    if (!state) throw new Error('dashboard: buildCtx sem state montado');
    return {
      state,
      panelWrap,
      v,
      esc,
      pickColor,
      cashAccounts,
      creditCards: creditCardAccounts,
      cbdAccounts,
      currentMonthTxs,
      allTxs: () => state?.data.transactions || [],
      categoryName,
      txIsExpense,
      expenseByCategory,
      monthlySeries,
      upcomingPayables,
      categoryBars,
      miniLineChart,
      stubPanel,
      SERIES_PALETTE,
      creditCardsApi: deps.creditCards,
      budgetApi: deps.budget,
    };
  }

  // ── Individual panel renderers ───────────────────────────────
  function renderPanel(p: PanelDef): JQuery {
    if (!state?.data.loaded) {
      return panelWrap({
        title: p.label,
        icon: p.icon,
        body: '<div style="font-size:12px;color:var(--text-muted);text-align:center;padding:20px 0;">Carregando…</div>',
      });
    }
    const renderer = PANELS[p.id];
    if (renderer) return renderer(p, buildCtx());
    return stubPanel(p);
  }

  // ── Render ───────────────────────────────────────────────────
  function render(): void {
    const $root = state?.$root;
    if (!$root || !state) return;
    $root.empty();

    const $header = pageHeader({
      title: 'Visão Geral',
      actions: [
        $(
          '<button class="icon-btn" data-act="toggle-hide" type="button" title="' +
            (state.hideValues ? 'Mostrar valores' : 'Ocultar valores') +
            '" style="width:34px;height:34px;">' +
            icon(state.hideValues ? 'eyeOff' : 'eye', 16) +
          '</button>',
        ),
        btn({ variant: 'secondary', size: 'sm', icon: 'settings', label: 'Personalizar', attrs: 'data-act="customize"' }),
      ],
    });
    $root.append($header);

    const order = state.settings.panelOrder && state.settings.panelOrder.length ? state.settings.panelOrder : ALL_PANELS.map((p) => p.id);

    const enabled = order
      .map((id) => ALL_PANELS.find((p) => p.id === id))
      .filter((p): p is PanelDef => !!p && !!state?.settings.enabled[p.id]);

    if (enabled.length === 0) {
      $root.append(emptyState({ icon: 'settings', title: 'Nenhum painel habilitado', desc: 'Clique em "Personalizar" para adicionar painéis à visão geral.' }));
      return;
    }

    const cols = state.settings.columns || 2;
    const $grid = $('<div class="dash-grid" style="display:grid;grid-template-columns:repeat(' + cols + ', 1fr);gap:14px;align-items:start;"></div>');

    enabled.forEach((p) => {
      const isDragged = state?.draggedId === p.id;
      const isDragOver = state?.dragOverId === p.id && !!state?.draggedId && state?.draggedId !== p.id;
      const $wrap = $(
        '<div data-panel="' + esc(p.id) + '" draggable="true" style="' +
          'opacity:' + (isDragged ? 0.35 : 1) + ';' +
          'transform:' + (isDragOver ? 'scale(1.01)' : 'none') + ';' +
          'transition:opacity 0.15s, transform 0.15s;' +
          'border-radius:var(--radius);' +
          'outline:' + (isDragOver ? '2px solid var(--accent)' : '2px solid transparent') + ';' +
        '"></div>',
      );
      $wrap.append(renderPanel(p));
      $grid.append($wrap);
    });

    $root.append($grid);
  }

  // ── Customize modal ──────────────────────────────────────────
  function openCustomizeModal(): void {
    if (!state) return;
    const local = cloneSettings(state.settings);

    function radioGroup(field: keyof DashboardSettings, options: readonly { value: string | number | boolean; label: string }[]): string {
      let html = '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-top:6px;">';
      options.forEach((opt) => {
        const key = field + '|' + opt.value;
        const isChecked = String(local[field]) === String(opt.value);
        html +=
          '<label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px;color:var(--text-primary);">' +
            '<input type="radio" data-radio-key="' + esc(key) + '" name="' + esc(field) + '" value="' + esc(opt.value) + '"' + (isChecked ? ' checked' : '') + ' style="accent-color:var(--accent);cursor:pointer;"/>' +
            esc(opt.label) +
          '</label>';
      });
      html += '</div>';
      return html;
    }

    function section(title: string, contentHtml: string, first?: boolean): string {
      return (
        '<div' + (first ? '' : ' style="border-top:1px solid var(--border);padding-top:16px;"') + '>' +
          '<p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:6px;">' + esc(title) + '</p>' +
          contentHtml +
        '</div>'
      );
    }

    let panelsListHtml = '<div style="display:flex;flex-direction:column;gap:6px;">';
    ALL_PANELS.forEach((pp) => {
      const checked = !!local.enabled[pp.id];
      panelsListHtml +=
        '<label data-panel-row="1" style="display:flex;align-items:center;gap:10px;padding:7px 10px;border-radius:var(--radius-sm);cursor:pointer;transition:background var(--transition);">' +
          '<input type="checkbox" data-panel-toggle="' + esc(pp.id) + '"' + (checked ? ' checked' : '') + ' style="width:15px;height:15px;accent-color:var(--accent);cursor:pointer;flex-shrink:0;"/>' +
          '<span style="font-size:13px;color:var(--text-primary);">' + esc(pp.label) + '</span>' +
        '</label>';
    });
    panelsListHtml += '</div>';

    const bodyHtml =
      '<div style="display:flex;flex-direction:column;gap:18px;">' +
        section(
          'Exibir painéis de receitas e despesas na visão de',
          radioGroup('viewMode', [
            { value: 'cash', label: 'Caixa' },
            { value: 'accrual', label: 'Competência' },
            { value: 'define', label: 'Definir em cada painel' },
          ]),
          true,
        ) +
        section(
          'Layout de exibição',
          radioGroup('columns', [
            { value: 1, label: '1 coluna' },
            { value: 2, label: '2 colunas' },
            { value: 3, label: '3 colunas' },
          ]),
        ) +
        section(
          'Utilizar barra de rolagem nos painéis',
          radioGroup('scrollPanels', [
            { value: true, label: 'Sim' },
            { value: false, label: 'Não' },
          ]),
        ) +
        section(
          'Considerar despesas e receitas vinculadas ao módulo de investimentos',
          radioGroup('includeInvestments', [
            { value: true, label: 'Sim' },
            { value: false, label: 'Não' },
          ]),
        ) +
        section('Habilitar e desabilitar painéis', panelsListHtml) +
      '</div>';

    const $cancel = btn({ variant: 'secondary', size: 'md', label: 'Cancelar', attrs: 'data-modal-close="1" type="button"' });
    const $save = btn({ variant: 'primary', size: 'md', label: 'Salvar', attrs: 'data-act="customize-save" type="button"' });
    const $footer = $('<div style="display:flex;gap:10px;"></div>').append($cancel).append($save);

    const m = modal({ title: 'Personalizar visão geral', body: bodyHtml, footer: $footer });
    m.open();

    function castValue(raw: string, current: unknown): string | number | boolean {
      if (typeof current === 'number') return parseInt(raw, 10);
      if (typeof current === 'boolean') return raw === 'true';
      return raw;
    }

    m.$body.on('change', 'input[type=radio]', function () {
      const $this = $(this);
      const field = $this.attr('name') as keyof DashboardSettings;
      const raw = $this.attr('value') || '';
      (local as unknown as Record<string, unknown>)[field] = castValue(raw, DEFAULT_SETTINGS[field]);
    });

    m.$body.on('change', 'input[data-panel-toggle]', function () {
      const id = $(this).attr('data-panel-toggle') as string;
      local.enabled = { ...local.enabled };
      local.enabled[id] = $(this).prop('checked');
    });

    m.$body
      .on('mouseenter', '[data-panel-row]', function () {
        $(this).css('background', 'var(--bg-hover)');
      })
      .on('mouseleave', '[data-panel-row]', function () {
        $(this).css('background', 'transparent');
      });

    m.$el.on('click', '[data-act=customize-save]', () => {
      const order = (local.panelOrder || []).slice();
      ALL_PANELS.forEach((p) => {
        if (order.indexOf(p.id) === -1) order.push(p.id);
      });
      local.panelOrder = order;
      if (state) {
        state.settings = local;
        saveSettings(state.settings);
      }
      m.close();
      render();
    });
  }

  // ── Drag & drop ──────────────────────────────────────────────
  function bindDragEvents($root: JQuery): void {
    $root.on('dragstart.dash', '[data-panel]', function (e) {
      const id = $(this).attr('data-panel') as string;
      if (state) state.draggedId = id;
      const dt = (e.originalEvent as DragEvent | undefined)?.dataTransfer;
      if (dt) {
        dt.effectAllowed = 'move';
        try {
          dt.setData('text/plain', id);
        } catch {
          /* noop */
        }
      }
      $(this).css('opacity', '0.35');
    });

    $root.on('dragover.dash', '[data-panel]', function (e) {
      e.preventDefault();
      const dt = (e.originalEvent as DragEvent | undefined)?.dataTransfer;
      if (dt) dt.dropEffect = 'move';
      const id = $(this).attr('data-panel') as string;
      if (state && state.draggedId && id !== state.draggedId && state.dragOverId !== id) {
        state.dragOverId = id;
        $root.find('[data-panel]').each(function () {
          const pid = $(this).attr('data-panel');
          const isOver = pid === state?.dragOverId && pid !== state?.draggedId;
          $(this).css({ outline: isOver ? '2px solid var(--accent)' : '2px solid transparent', transform: isOver ? 'scale(1.01)' : 'none' });
        });
      }
    });

    $root.on('drop.dash', '[data-panel]', function (e) {
      e.preventDefault();
      if (!state) return;
      const targetId = $(this).attr('data-panel') as string;
      const draggedId = state.draggedId;
      if (!draggedId || draggedId === targetId) {
        state.draggedId = null;
        state.dragOverId = null;
        render();
        return;
      }
      const order = state.settings.panelOrder && state.settings.panelOrder.length ? state.settings.panelOrder.slice() : ALL_PANELS.map((p) => p.id);
      const fromIdx = order.indexOf(draggedId);
      const toIdx = order.indexOf(targetId);
      if (fromIdx !== -1 && toIdx !== -1) {
        order.splice(fromIdx, 1);
        order.splice(toIdx, 0, draggedId);
        state.settings.panelOrder = order;
        saveSettings(state.settings);
      }
      state.draggedId = null;
      state.dragOverId = null;
      render();
    });

    $root.on('dragend.dash', '[data-panel]', () => {
      if (!state) return;
      state.draggedId = null;
      state.dragOverId = null;
      render();
    });
  }

  // ── Event delegation ─────────────────────────────────────────
  function bindRoot($root: JQuery): void {
    $root.on('click.dash', '[data-act=toggle-hide]', () => {
      if (!state) return;
      state.hideValues = !state.hideValues;
      render();
    });
    $root.on('click.dash', '[data-act=customize]', () => openCustomizeModal());
    $root.on('click.dash', '[data-act=goto-tx]', () => {
      window.location.hash = '#/transactions';
    });
    bindDragEvents($root);
  }

  return createPage<DashboardPageState>({
    ns: '.dash',
    state: () => {
      state = {
        settings: loadSettings(),
        hideValues: false,
        draggedId: null,
        dragOverId: null,
        data: { transactions: [], payables: [], receivables: [], loaded: false },
      };
      return state;
    },
    render,
    bind: bindRoot,
    onMount: (s) => {
      loadAll().then(() => {
        if (state && state.$root === s.$root) render();
      });
      s.unsubscribeAcc = deps.accounts.onChange(() => render());
      s.unsubscribeCat = deps.categories.onChange(() => render());
    },
    onUnmount: (s) => {
      if (s.unsubscribeAcc) s.unsubscribeAcc();
      if (s.unsubscribeCat) s.unsubscribeCat();
    },
  });
}

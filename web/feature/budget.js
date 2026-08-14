/* feature/budget.js — fatia Metas/Orçamento. Um arquivo por fatia: domain (regras puras de item
 * de orçamento) → infrastructure/secondary (BudgetRepository, adapter HTTP /budget) →
 * application (BudgetService) → infrastructure/primary (página), cada bloco abaixo é um IIFE
 * independente (comentário original de cada arquivo preservado como separador de seção).
 * Consumida por dashboard (painel de metas de despesa) via budget.api.js — achado ao migrar
 * dashboard, não fazia parte do roadmap original. */
/* _1_domain/budget.js — Budget item rules. Pure. */
(function () {
  function normalize(raw) {
    if (!raw) return null;
    const budgeted = +(raw.budgeted != null ? raw.budgeted : (raw.budget != null ? raw.budget : raw.target)) || 0;
    const spent    = +(raw.spent != null ? raw.spent : raw.actual) || 0;
    return {
      id:         raw.id,
      categoryId: raw.categoryId,
      name:       raw.name || '',
      budgeted:   budgeted,
      spent:      spent,
      month:      raw.month,
      year:       raw.year,
      color:      raw.color || null,
      icon:       raw.icon || null,
    };
  }

  /* Consumption percent, clamped to [0, 100]. */
  function consumptionPct(spent, budgeted) {
    const b = +budgeted || 0;
    if (b <= 0) return 0;
    return Math.min(100, Math.max(0, (Math.abs(+spent || 0) / b) * 100));
  }

  function isOverBudget(spent, budgeted) {
    const b = +budgeted || 0;
    return b > 0 && Math.abs(+spent || 0) > b;
  }

  /* Returns { value, over } where `value` is the remaining (positive) or
     the over-spent amount (positive) and `over` flags which one. */
  function remainingOrOver(spent, budgeted) {
    const b = +budgeted || 0;
    const s = Math.abs(+spent || 0);
    if (s > b) return { value: s - b, over: true };
    return { value: b - s, over: false };
  }

  /* Bar color tokens by consumption percent. Mirrors STYLE.md §11 (kernel-shared threshold). */
  function barColor(pct) {
    return window.thresholdColorToken(pct);
  }

  function overspendCount(items) {
    return (items || []).reduce(function (acc, b) {
      return acc + (isOverBudget(b.spent, b.budgeted) ? 1 : 0);
    }, 0);
  }

  window.Domain = window.Domain || {};
  window.Domain.Budget = {
    normalize: normalize,
    consumptionPct: consumptionPct,
    isOverBudget: isOverBudget,
    remainingOrOver: remainingOrOver,
    barColor: barColor,
    overspendCount: overspendCount,
  };
})();
/* _3_infrastructure/secondary/budget-repository.js — HTTP adapter for /budget. */
(function () {
  function create(http) {
    return {
      list:   function (month, year) { return http.get('/budget?month=' + month + '&year=' + year); },
      create: function (data)        { return http.post('/budget', data); },
      update: function (id, data)    { return http.patch('/budget/' + id, data); },
      remove: function (id)          { return http.delete('/budget/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.BudgetRepository = { create: create };
})();
/* _2_application/budget-service.js — Budget use cases. */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  function loadPeriod(period) {
    return repo.list(period.month, period.year);
  }

  function save(id, data) { return repo.update(id, data); }
  function create(data)   { return repo.create(data); }
  function remove(id)     { return repo.remove(id); }

  function summary(items) {
    return {
      total:        (items || []).length,
      overspending: window.Domain.Budget.overspendCount(items),
    };
  }

  window.App = window.App || {};
  window.App.BudgetService = {
    init: init,
    loadPeriod: loadPeriod,
    save: save,
    create: create,
    remove: remove,
    summary: summary,
  };
})();
/* pages/budget.js — Metas / Orçamento (lista de metas + CRUD modal). */
(function () {
  window.Pages = window.Pages || {};

  // Icon options for the modal (uses inline ICONS set from icons.js).
  const ICON_CHOICES = [
    'list', 'tag', 'creditCard', 'building', 'briefcase',
    'home', 'target', 'pieChart', 'activity', 'barChart',
    'calendar', 'trendingUp', 'trendingDown', 'dollarSign', 'hash'
  ];

  // ── Per-mount state ─────────────────────────────────────
  let state = null;

  function resetState() {
    const p = window.App.PeriodService.get(); // { month: 1-12, year }
    state = {
      $root: null,
      loading: true,
      items: [],
      month: p.month - 1,      // 0-based
      year: p.year,
    };
    return state;
  }


  // ── Helpers ─────────────────────────────────────────────

  function shiftMonth(delta) { window.shiftMonth(state, delta, false); }

  function colorFor(item, idx) {
    return item.color || window.PALETTE.swatches[idx % window.PALETTE.swatches.length];
  }

  function iconFor(item) {
    // Backend may send a glyph (emoji) or icon-name. We always render an inline SVG;
    // if the value matches a known ICONS key, use it, otherwise fall back to 'target'.
    const name = item && item.icon;
    if (name && window.ICONS && window.ICONS[name]) return name;
    return 'target';
  }

  function findById(id) { return window.byId(state.items, id); }

  function categoryName(id) {
    const c = window.App.CacheStore.findById('categories', id);
    return c ? c.name : null;
  }

  // ── Data ────────────────────────────────────────────────
  function loadBudget() {
    state.loading = true;
    render();
    const period = window.Domain.Period.create(state.month + 1, state.year);
    return window.App.BudgetService.loadPeriod(period).then(function (list) {
      state.items = Array.isArray(list) ? list.slice() : [];
      state.loading = false;
      render();
    }).catch(function (err) {
      state.loading = false;
      state.items = [];
      render();
      window.toast((err && err.message) || 'Falha ao carregar metas');
    });
  }

  // ── Render ──────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;
    $root.empty();

    // Header
    const $header = window.pageHeader({
      title: 'Metas / Orçamento',
      actions: window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Meta',
        attrs: 'data-act="new"',
      }),
      nav: window.periodNav({
        month: state.month + 1,
        year: state.year,
        onPrev: function () { shiftMonth(-1); loadBudget(); },
        onNext: function () { shiftMonth(+1); loadBudget(); },
        onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m - 1; state.year = y; loadBudget(); },
      })
    });
    $root.append($header);

    // Summary cards
    let totalBudgeted = 0, totalSpent = 0;
    state.items.forEach(function (it) {
      const b = +(it.budgeted || it.budget || it.target || 0);
      const s = +(it.spent || it.actual || 0);
      totalBudgeted += b;
      totalSpent += s;
    });
    const overCount = window.Domain.Budget.overspendCount(state.items.map(window.Domain.Budget.normalize));
    const pctTotal  = window.Domain.Budget.consumptionPct(totalSpent, totalBudgeted);

    const summaryHtml =
      '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-bottom:20px;">' +
        window.statCardHtml({ label: 'Orçamento Total', value: window.fmt(totalBudgeted), color: 'var(--text-primary)' }) +
        window.statCardHtml({
          label: 'Total Gasto', value: window.fmt(totalSpent),
          color: totalSpent > totalBudgeted && totalBudgeted > 0 ? 'var(--expense)' : 'var(--text-primary)',
          sub: totalBudgeted > 0 ? pctTotal.toFixed(0) + '% do orçamento' : '—',
        }) +
        window.statCardHtml({
          label: 'Categorias Estouradas', value: String(overCount),
          color: overCount > 0 ? 'var(--expense)' : 'var(--income)',
        }) +
      '</div>';
    $root.append(summaryHtml);

    // List
    if (state.loading) {
      $root.append(window.emptyState({ icon: 'target', title: 'Carregando…' }));
      return;
    }
    if (!state.items.length) {
      $root.append(window.emptyState({
        icon: 'target',
        title: 'Nenhuma meta cadastrada',
        desc: 'Clique em “Nova Meta” para configurar o orçamento deste mês.',
      }));
      return;
    }

    const $list = $('<div style="display:flex;flex-direction:column;gap:12px;"></div>');
    state.items.forEach(function (it, idx) {
      $list.append(renderRow(it, idx));
    });
    $root.append($list);
  }

  function renderRow(item, idx) {
    const name = item.name || categoryName(item.categoryId) || 'Meta sem categoria';
    const budgeted = +(item.budgeted || item.budget || item.target || 0);
    const spent = +(item.spent || item.actual || 0);
    const pctRaw = budgeted > 0 ? (spent / budgeted) * 100 : 0;
    const pct = window.Domain.Budget.consumptionPct(spent, budgeted);
    const over = window.Domain.Budget.isOverBudget(spent, budgeted);
    const barColor = 'var(--' + window.Domain.Budget.barColor(pctRaw) + ')';
    const color = colorFor(item, idx);
    const icName = iconFor(item);
    const diff = budgeted - spent;

    const $card = $('<div class="card" data-id="' + esc(item.id) + '" style="padding:16px 20px;display:flex;flex-direction:column;gap:10px;"></div>');

    const headHtml =
      '<div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">' +
        '<div style="display:flex;align-items:center;gap:10px;min-width:0;">' +
          '<span style="width:32px;height:32px;border-radius:8px;background:' + esc(color) + '22;' +
            'color:' + esc(color) + ';display:inline-flex;align-items:center;justify-content:center;flex-shrink:0;">' +
            window.icon(icName, 16) +
          '</span>' +
          '<span style="font-size:14px;font-weight:700;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' +
            esc(name) +
          '</span>' +
          (over ? ' ' + window.badge('Estourado', 'expense') : '') +
        '</div>' +
        '<div style="display:flex;align-items:center;gap:14px;">' +
          '<div style="text-align:right;display:flex;gap:6px;align-items:baseline;">' +
            '<span style="font-size:13px;font-weight:700;color:' + (over ? 'var(--expense)' : 'var(--text-primary)') + ';">' +
              esc(window.fmt(spent)) +
            '</span>' +
            '<span style="font-size:12px;color:var(--text-secondary);font-weight:600;">' +
              '/ ' + esc(window.fmt(budgeted)) +
            '</span>' +
          '</div>' +
          '<div data-region="row-actions" style="display:flex;gap:2px;"></div>' +
        '</div>' +
      '</div>';
    $card.append(headHtml);

    // Progress bar
    $card.append(window.progressBarHtml(pct, barColor));

    // Footer line
    const footerHtml =
      '<div style="display:flex;justify-content:space-between;font-size:11px;">' +
        '<span style="color:var(--text-muted);">' + (budgeted > 0 ? pctRaw.toFixed(0) + '% utilizado' : 'Sem orçamento') + '</span>' +
        '<span style="font-weight:600;color:' + (over ? 'var(--expense)' : 'var(--income)') + ';">' +
          (over
            ? esc(window.fmt(diff)) + ' acima'
            : esc(window.fmt(Math.max(0, diff))) + ' restante') +
        '</span>' +
      '</div>';
    $card.append(footerHtml);

    // Row actions
    const $actions = $card.find('[data-region=row-actions]');
    $actions.append(window.rowActionBtn('edit', 'Editar', item.id));
    $actions.append(window.rowActionBtn('trash', 'Excluir', item.id, true));

    return $card;
  }


  // ── Modal: create / edit ────────────────────────────────
  function openFormModal(existing) {
    const isEdit = !!existing;
    const initBudgeted = isEdit ? +(existing.budgeted || existing.budget || existing.target || 0) : 0;
    const initCategoryId = isEdit ? (existing.categoryId || '') : '';
    const initColor = isEdit ? (existing.color || window.PALETTE.swatches[0]) : window.PALETTE.swatches[0];
    const initIcon  = isEdit ? iconFor(existing) : 'target';

    const uid = 'bd-' + Date.now();
    const amountInitial = initBudgeted > 0 ? window.maskCurrency(initBudgeted) : '';

    // keepId: em edição, categoria já gravada continua na lista mesmo se foi inativada depois —
    // o campo fica disabled (categoria não muda pós-criação), mas não pode sumir da tela.
    const categoryFieldHtml = window.categoryPickerHtml({
      items: flatCategories('EXPENSE', true, initCategoryId),
      selectedId: initCategoryId,
      selectId: uid + '-cat',
      selectAttrs: ' name="categoryId"' + (isEdit ? ' disabled' : ''),
      placeholder: '— Selecione —',
      disabled: isEdit,
    });

    const colorSwatches = window.PALETTE.swatches.map(function (c) {
      const active = c === initColor;
      return (
        '<button type="button" class="swatch swatch-lg' + (active ? ' is-active' : '') + '" ' +
          'data-color-pick="' + esc(c) + '" style="background:' + esc(c) + ';"></button>'
      );
    }).join('');

    const iconChoices = ICON_CHOICES.map(function (n) {
      const active = n === initIcon;
      return (
        '<button type="button" class="icon-btn icon-pick' + (active ? ' is-active' : '') + '" ' +
          'data-icon-pick="' + esc(n) + '">' +
          window.icon(n, 16) +
        '</button>'
      );
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

    const m = window.formModal({
      title: isEdit ? 'Editar Meta' : 'Nova Meta',
      formName: 'bd',
      body: bodyHtml,
      footer: { saveAttrs: 'data-act="save" type="button"' },
      onSubmit: function ($form) {
        const categoryId = $form.find('select[name=categoryId]').val() || '';
        const amountRaw = $form.find('input[name=budgeted]').val() || '';
        const color = $form.find('input[name=color]').val() || window.PALETTE.swatches[0];
        const iconName = $form.find('input[name=icon]').val() || 'target';
        const budgeted = window.parseCurrency(amountRaw);

        if (!isEdit && !categoryId) {
          window.toast('Selecione uma categoria');
          $form.find('select[name=categoryId]').trigger('focus');
          return null;
        }
        if (!isFinite(budgeted) || budgeted <= 0) {
          window.toast('Informe um valor válido');
          $form.find('input[name=budgeted]').trigger('focus');
          return null;
        }

        const payload = { budgeted: budgeted, color: color, icon: iconName };
        if (isEdit) return window.App.BudgetService.save(existing.id, payload);
        payload.categoryId = categoryId;
        payload.month = state.month + 1;
        payload.year = state.year;
        return window.App.BudgetService.create(payload);
      },
      success: function () { return isEdit ? 'Meta atualizada' : 'Meta criada'; },
      failure: 'Falha ao salvar meta',
      onDone: loadBudget,
    });

    // Bind currency mask
    window.bindCurrencyMask(m.$body.find('input[name=budgeted]'));

    // Color swatch picker
    m.$body.on('click', '[data-color-pick]', function () {
      const c = $(this).attr('data-color-pick');
      m.$body.find('input[name=color]').val(c);
      m.$body.find('[data-color-pick]').each(function () {
        $(this).toggleClass('is-active', $(this).attr('data-color-pick') === c);
      });
    });

    // Icon picker
    m.$body.on('click', '[data-icon-pick]', function () {
      const name = $(this).attr('data-icon-pick');
      m.$body.find('input[name=icon]').val(name);
      m.$body.find('[data-icon-pick]').each(function () {
        $(this).toggleClass('is-active', $(this).attr('data-icon-pick') === name);
      });
    });

  }

  // ── Modal: confirm delete ───────────────────────────────
  function openDeleteModal(target) {
    const name = target.name || categoryName(target.categoryId) || 'esta meta';
    window.confirmModal({
      title: 'Excluir Meta',
      body: window.modalText('Tem certeza que deseja excluir a meta <strong>' + esc(name) + '</strong>? Esta ação não pode ser desfeita.'),
      onConfirm: function (m, reEnable) {
        window.runMutation(window.App.BudgetService.remove(target.id), {
          modal: m, success: 'Meta excluída', failure: 'Falha ao excluir meta',
          onDone: loadBudget, onError: reEnable,
        });
      },
    });
  }

  // ── Event delegation ────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.bd', '[data-act=new]', function () {
      openFormModal(null);
    });
    $root.on('click.bd', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const it = findById(id);
      if (it) openFormModal(it);
    });
    $root.on('click.bd', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const it = findById(id);
      if (it) openDeleteModal(it);
    });
  }

  // ── Lifecycle ───────────────────────────────────────────
  window.Pages['budget'] = window.page({
    ns: '.bd',
    state: resetState,
    render: render,
    bind: bindRoot,
    onMount: loadBudget,
  });
})();

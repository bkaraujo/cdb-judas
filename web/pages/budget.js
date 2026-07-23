/* pages/budget.js — Metas / Orçamento (lista de metas + CRUD modal). */
(function () {
  window.Pages = window.Pages || {};

  // Color palette for new budgets / fallback.
  const PALETTE = [
    '#6366F1', '#38BDF8', '#F59E0B', '#10B981',
    '#F43F5E', '#A78BFA', '#FB923C', '#34D399',
    '#820AD1', '#1C2951'
  ];

  // Icon options for the modal (uses inline ICONS set from icons.js).
  const ICON_CHOICES = [
    'list', 'tag', 'creditCard', 'building', 'briefcase',
    'home', 'target', 'pieChart', 'activity', 'barChart',
    'calendar', 'trendingUp', 'trendingDown', 'dollarSign', 'hash'
  ];

  // ── Per-mount state ─────────────────────────────────────
  let state = null;

  function resetState() {
    state = {
      $root: null,
      loading: true,
      items: [],
      month: 0,      // 0-based
      year: 0,
    };
    const now = new Date();
    state.month = now.getMonth();
    state.year = now.getFullYear();
  }


  // ── Helpers ─────────────────────────────────────────────

  function shiftMonth(delta) { window.shiftMonth(state, delta, false); }

  function colorFor(item, idx) {
    return item.color || PALETTE[idx % PALETTE.length];
  }

  function iconFor(item) {
    // Backend may send a glyph (emoji) or icon-name. We always render an inline SVG;
    // if the value matches a known ICONS key, use it, otherwise fall back to 'target'.
    const name = item && item.icon;
    if (name && window.ICONS && window.ICONS[name]) return name;
    return 'target';
  }

  function findById(id) { return window.byId(state.items, id); }

  function categoryOptions(selectedId) {
    const expenseCats = window.Domain.Category.filterByNature(
      window.App.CacheStore.categories(), 'EXPENSE'
    ).slice().sort(window.sortByName);
    let html = '<option value="">— Selecione —</option>';
    expenseCats.forEach(function (c) {
      const sel = String(c.id) === String(selectedId) ? ' selected' : '';
      html += '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.name) + '</option>';
    });
    return html;
  }

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
    const $header = $(
      '<div class="page-header">' +
        '<h1>Metas / Orçamento</h1>' +
        '<div class="page-header-actions" data-region="actions"></div>' +
      '</div>'
    );
    const $actions = $header.find('[data-region=actions]');

    const $pnav = window.periodNav({
      month: state.month + 1,
      year: state.year,
      onPrev: function () { shiftMonth(-1); loadBudget(); },
      onNext: function () { shiftMonth(+1); loadBudget(); },
      onChange: function (m, y) { state.month = m - 1; state.year = y; loadBudget(); },
    });
    $actions.append($pnav);
    $actions.append(window.btn({
      variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Meta',
      attrs: 'data-act="new"',
    }));
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
        summaryCard('Orçamento Total', window.fmt(totalBudgeted), 'var(--text-primary)') +
        summaryCard('Total Gasto', window.fmt(totalSpent),
          totalSpent > totalBudgeted && totalBudgeted > 0 ? 'var(--expense)' : 'var(--text-primary)',
          totalBudgeted > 0 ? pctTotal.toFixed(0) + '% do orçamento' : '—'
        ) +
        summaryCard('Categorias Estouradas', String(overCount),
          overCount > 0 ? 'var(--expense)' : 'var(--income)'
        ) +
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

  function summaryCard(label, value, valueColor, sub) {
    return (
      '<div class="card" style="padding:16px 20px;">' +
        '<p style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px;">' +
          esc(label) +
        '</p>' +
        '<p style="font-size:22px;font-weight:800;color:' + valueColor + ';">' + esc(value) + '</p>' +
        (sub
          ? '<p style="font-size:12px;color:var(--text-muted);margin-top:4px;">' + esc(sub) + '</p>'
          : '') +
      '</div>'
    );
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
    const barHtml =
      '<div style="height:8px;background:var(--bg-hover);border-radius:4px;overflow:hidden;">' +
        '<div style="height:100%;border-radius:4px;width:' + pct + '%;' +
          'background:' + barColor + ';transition:width 0.5s ease;"></div>' +
      '</div>';
    $card.append(barHtml);

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
    const initColor = isEdit ? (existing.color || PALETTE[0]) : PALETTE[0];
    const initIcon  = isEdit ? iconFor(existing) : 'target';

    const uid = 'bd-' + Date.now();
    const amountInitial = initBudgeted > 0 ? window.maskCurrency(initBudgeted) : '';

    const colorSwatches = PALETTE.map(function (c) {
      const active = c === initColor;
      return (
        '<button type="button" data-color-pick="' + esc(c) + '" ' +
          'style="width:28px;height:28px;border-radius:50%;background:' + esc(c) + ';' +
          'border:2px solid ' + (active ? 'var(--text-primary)' : 'transparent') + ';' +
          'cursor:pointer;flex-shrink:0;transition:transform var(--transition);"></button>'
      );
    }).join('');

    const iconChoices = ICON_CHOICES.map(function (n) {
      const active = n === initIcon;
      return (
        '<button type="button" data-icon-pick="' + esc(n) + '" ' +
          'class="icon-btn" style="width:34px;height:34px;' +
          'background:' + (active ? 'var(--accent-light)' : 'transparent') + ';' +
          'color:' + (active ? 'var(--accent)' : 'var(--text-secondary)') + ';' +
          'border:1px solid ' + (active ? 'var(--accent)' : 'var(--border)') + ';">' +
          window.icon(n, 16) +
        '</button>'
      );
    }).join('');

    const bodyHtml =
      '<form data-form="bd" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + uid + '-cat">Categoria</label>' +
            '<select id="' + uid + '-cat" name="categoryId"' + (isEdit ? ' disabled' : '') + '>' +
              categoryOptions(initCategoryId) +
            '</select>' +
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

    const m = window.modal({
      title: isEdit ? 'Editar Meta' : 'Nova Meta',
      body: bodyHtml,
      footer: window.saveCancelFooter({ saveAttrs: 'data-act="save" type="button"' }),
    });
    m.open();

    // Bind currency mask
    window.bindCurrencyMask(m.$body.find('input[name=budgeted]'));

    // Color swatch picker
    m.$body.on('click', '[data-color-pick]', function () {
      const c = $(this).attr('data-color-pick');
      m.$body.find('input[name=color]').val(c);
      m.$body.find('[data-color-pick]').each(function () {
        const active = $(this).attr('data-color-pick') === c;
        $(this).css('border', '2px solid ' + (active ? 'var(--text-primary)' : 'transparent'));
      });
    });

    // Icon picker
    m.$body.on('click', '[data-icon-pick]', function () {
      const name = $(this).attr('data-icon-pick');
      m.$body.find('input[name=icon]').val(name);
      m.$body.find('[data-icon-pick]').each(function () {
        const active = $(this).attr('data-icon-pick') === name;
        $(this).css({
          background: active ? 'var(--accent-light)' : 'transparent',
          color:      active ? 'var(--accent)'       : 'var(--text-secondary)',
          border:     '1px solid ' + (active ? 'var(--accent)' : 'var(--border)'),
        });
      });
    });

    function submit(e) {
      if (e) e.preventDefault();
      const $form = m.$body.find('form[data-form=bd]');
      const categoryId = $form.find('select[name=categoryId]').val() || '';
      const amountRaw = $form.find('input[name=budgeted]').val() || '';
      const color = $form.find('input[name=color]').val() || PALETTE[0];
      const iconName = $form.find('input[name=icon]').val() || 'target';
      const budgeted = window.parseCurrency(amountRaw);

      if (!isEdit && !categoryId) {
        window.toast('Selecione uma categoria');
        $form.find('select[name=categoryId]').trigger('focus');
        return;
      }
      if (!isFinite(budgeted) || budgeted <= 0) {
        window.toast('Informe um valor válido');
        $form.find('input[name=budgeted]').trigger('focus');
        return;
      }

      const $btn = m.$el.find('[data-act=save]').prop('disabled', true);

      const payload = {
        budgeted: budgeted,
        color: color,
        icon: iconName,
      };
      let p;
      if (isEdit) {
        p = window.App.BudgetService.save(existing.id, payload);
      } else {
        payload.categoryId = categoryId;
        payload.month = state.month + 1;
        payload.year = state.year;
        p = window.App.BudgetService.create(payload);
      }

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Meta atualizada' : 'Meta criada', 'success');
        return loadBudget();
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao salvar meta');
      });
    }

    m.$body.find('form[data-form=bd]').on('submit', submit);
    m.$el.on('click', '[data-act=save]', submit);
  }

  // ── Modal: confirm delete ───────────────────────────────
  function openDeleteModal(target) {
    const name = target.name || categoryName(target.categoryId) || 'esta meta';
    window.confirmModal({
      title: 'Excluir Meta',
      body: window.modalText('Tem certeza que deseja excluir a meta <strong>' + esc(name) + '</strong>? Esta ação não pode ser desfeita.'),
      onConfirm: function (m, reEnable) {
        window.App.BudgetService.remove(target.id).then(function () {
          m.close();
          window.toast('Meta excluída', 'success');
          return loadBudget();
        }).catch(function (err) {
          reEnable();
          window.toast((err && err.message) || 'Falha ao excluir meta');
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
  window.Pages['budget'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      render();
      loadBudget();
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.bd');
      }
      state = null;
    },
  };
})();

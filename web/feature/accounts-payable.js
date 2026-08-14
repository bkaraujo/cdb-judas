/* feature/accounts-payable.js — fatia A Pagar/Receber, vista filtrada sobre transactions (sem
 * recurso backend próprio — A Pagar/Receber É uma transação pendente/agendada). Um arquivo por
 * fatia: domain → application → infrastructure/primary, cada bloco abaixo é um IIFE
 * independente (comentário original de cada arquivo preservado como separador de seção). Sem
 * secondary própria: lê e escreve via TransactionsApi (fatia irmã) — fecha V5. */

/* domain — Payable / Receivable rules. Pure. */
(function () {
  const TYPES  = { PAYABLE: 'PAYABLE', RECEIVABLE: 'RECEIVABLE' };
  const STATUS = {
    PENDING:   'pending',
    SCHEDULED: 'scheduled',
    CONFIRMED: 'confirmed',
    CANCELLED: 'cancelled',
  };

  function normalize(raw) {
    if (!raw) return null;
    return {
      id:         raw.id,
      name:       raw.name || raw.description || '',
      amount:     +raw.amount || 0,
      due:        raw.due || raw.date || null,
      status:     String(raw.status || STATUS.PENDING).toLowerCase(),
      categoryId: raw.categoryId,
      accountId:  raw.accountId,
      type:       String(raw.type || TYPES.PAYABLE).toUpperCase(),
    };
  }

  function isReceivable(p) { return !!p && p.type === TYPES.RECEIVABLE; }
  function isPayable(p)    { return !!p && p.type === TYPES.PAYABLE; }
  function isActive(p)     { return !!p && p.status !== STATUS.CANCELLED; }

  /* Transaction nature derived from payable type. */
  function natureOf(typeOrPayable) {
    const t = typeof typeOrPayable === 'string' ? typeOrPayable : (typeOrPayable && typeOrPayable.type);
    return String(t || '').toUpperCase() === TYPES.RECEIVABLE ? 'income' : 'expense';
  }

  /* Filter list by due date inside a Period. */
  function inPeriod(items, period) {
    return (items || []).filter(function (p) {
      if (!p.due) return false;
      return window.Domain.Period.containsDate(period, p.due);
    });
  }

  function periodTotals(items, period) {
    const inP = inPeriod(items, period);
    let payable = 0, receivable = 0;
    for (let i = 0; i < inP.length; i++) {
      const p = inP[i];
      if (!isActive(p)) continue;
      const v = Math.abs(p.amount);
      if (isReceivable(p)) receivable += v; else payable += v;
    }
    return { payable: payable, receivable: receivable, result: receivable - payable };
  }

  const statusBadgeVariant = window.Domain.Transaction.statusBadgeVariant;

  window.Domain = window.Domain || {};
  window.Domain.Payable = {
    TYPES: TYPES,
    STATUS: STATUS,
    normalize: normalize,
    isReceivable: isReceivable,
    isPayable: isPayable,
    isActive: isActive,
    natureOf: natureOf,
    inPeriod: inPeriod,
    periodTotals: periodTotals,
    statusBadgeVariant: statusBadgeVariant,
  };
})();

/* _2_application/payable-service.js — "A Pagar / A Receber" como filtro de transações pendentes.
 * Não há recurso de payables: A Pagar = despesas pendentes; A Receber = receitas pendentes. */
(function () {
  let txRepo = null;

  function init(deps) { txRepo = deps.repo; return { ready: true }; }

  // Adapta a transação ao formato consumido pela tela (Domain.Payable): due, amount positivo, type label.
  function adapt(label) {
    return function (txs) {
      return (Array.isArray(txs) ? txs : []).map(function (t) {
        return {
          id: t.id,
          description: t.description,
          due: t.date,
          amount: Math.abs(+t.amount || 0),
          accountId: t.accountId,
          categoryId: t.categoryId,
          status: t.status,
          type: label,
          groupId: t.groupId,
          totalInstallments: t.totalInstallments,
          installmentNumber: t.installmentNumber,
        };
      });
    };
  }

  function listPayable()    { return txRepo.list('status=pending&type=expense').then(adapt('PAYABLE')); }
  function listReceivable() { return txRepo.list('status=pending&type=income').then(adapt('RECEIVABLE')); }

  function periodTotals(items, period) { return window.Domain.Payable.periodTotals(items, period); }
  function inPeriod(items, period)     { return window.Domain.Payable.inPeriod(items, period); }

  window.App = window.App || {};
  window.App.PayableService = {
    init: init,
    listPayable: listPayable,
    listReceivable: listReceivable,
    periodTotals: periodTotals,
    inPeriod: inPeriod,
  };
})();

/* pages/accounts-payable.js — A Pagar e Receber.
 * Lists from API.accountsPayable (listPayable + listReceivable), filters by period (month/year on `due`).
 * Summary card (3-col), tabs (pagar/receber), card-list rows with status dot + badge + amount + actions.
 * CRUD via API.transactions (create/update/remove). Mark-as-paid via API.transactions.patchStatus.
 */
(function () {
  window.Pages = window.Pages || {};

  // ── State ─────────────────────────────────────────────────
  let state = null;

  function resetState() {
    const p = window.App.PeriodService.get(); // { month: 1-12, year }
    state = {
      $root: null,
      loading: true,
      payable: [],
      receivable: [],
      tab: 'payable',       // 'payable' | 'receivable'
      month: p.month - 1,
      year: p.year,
    };
  }

  // ── Helpers ───────────────────────────────────────────────



  function currentPeriod() {
    return window.Domain.Period.create(state.month + 1, state.year);
  }

  function findItem(id) { return window.byId(state.payable.concat(state.receivable), id); }

  function inPeriod(arr) {
    return window.App.PayableService.inPeriod(arr, currentPeriod());
  }

  // ── Fetch ─────────────────────────────────────────────────
  function loadData() {
    state.loading = true;
    render();
    return Promise.all([
      window.App.PayableService.listPayable(),
      window.App.PayableService.listReceivable(),
    ]).then(function (arr) {
      state.payable    = Array.isArray(arr[0]) ? arr[0] : [];
      state.receivable = Array.isArray(arr[1]) ? arr[1] : [];
      state.loading = false;
      render();
    }).catch(function (err) {
      state.loading = false;
      state.payable = [];
      state.receivable = [];
      render();
      window.toast((err && err.message) || 'Falha ao carregar contas', 'error');
    });
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $page = $('<div class="fade-in"></div>');

    // ── Page header ──
    const $header = window.pageHeader({
      title: 'A Pagar e Receber',
      nav: window.periodNav({
        month: state.month + 1,
        year: state.year,
        onPrev: function () { window.shiftMonth(state, -1, false); render(); },
        onNext: function () { window.shiftMonth(state, +1, false); render(); },
        onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m - 1; state.year = y; render(); },
      })
    });

    $page.append($header);

    // ── Summary card (3-col, divided) ──
    const periodPayable    = inPeriod(state.payable);
    const periodReceivable = inPeriod(state.receivable);
    const totalPayable     = periodPayable.reduce(function (s, i) { return s + (Number(i.amount) || 0); }, 0);
    const totalReceivable  = periodReceivable.reduce(function (s, i) { return s + (Number(i.amount) || 0); }, 0);
    const result           = totalReceivable - totalPayable;
    const resultColor      = result >= 0 ? 'var(--income)' : 'var(--expense)';

    function col(label, value, color) {
      return window.statCardHtml({ label: label, value: fmt(value), color: color, bare: true });
    }
    const $summary = $(
      '<div class="card" style="padding:16px 20px;margin-bottom:16px;display:flex;gap:32px;align-items:center;">' +
        col('A Pagar',   totalPayable,    'var(--expense)') +
        '<div style="width:1px;align-self:stretch;background:var(--border);"></div>' +
        col('A Receber', totalReceivable, 'var(--income)') +
        '<div style="width:1px;align-self:stretch;background:var(--border);"></div>' +
        col('Resultado', result,          resultColor) +
      '</div>'
    );
    $page.append($summary);

    // ── Tabs ──
    const $tabs = window.tabs(
      [
        { id: 'payable',    label: 'A pagar ('   + periodPayable.length    + ')' },
        { id: 'receivable', label: 'A receber (' + periodReceivable.length + ')' },
      ],
      state.tab,
      function (id) { state.tab = id; render(); }
    );
    $page.append($tabs);

    // ── List ──
    const items = state.tab === 'payable' ? periodPayable : periodReceivable;
    $page.append(renderList(items));

    $root.empty().append($page);
  }

  function renderList(items) {
    const $card = $(
      '<div class="card card-list" style="overflow-y:auto;max-height:calc(100vh - 320px);min-height:200px;"></div>'
    );

    if (state.loading) {
      $card.append(window.emptyState({ icon: 'calendar', title: 'Carregando…' }));
      return $card;
    }
    if (items.length === 0) {
      $card.append(window.emptyState({
        icon: 'calendar',
        title: 'Nenhuma conta neste período',
        desc: state.tab === 'payable'
          ? 'Nenhuma conta a pagar para o período selecionado.'
          : 'Nenhuma conta a receber para o período selecionado.',
      }));
      return $card;
    }

    const catMap = categoryById();
    const sorted = items.slice().sort(function (a, b) {
      return String(a.due).localeCompare(String(b.due));
    });

    sorted.forEach(function (item) {
      const st = String(item.status || 'pending').toLowerCase();
      const badgeColor = window.Domain.Payable.statusBadgeVariant(st);
      const dotColor   = 'var(--' + badgeColor + ')';
      const badgeLabel = st === 'scheduled' ? 'Agendado'
                     : st === 'confirmed' ? 'Confirmado'
                     : 'Pendente';

      const amtColor = state.tab === 'payable' ? 'var(--expense)' : 'var(--income)';
      const amtAbs = Math.abs(Number(item.amount) || 0);

      const cat = catMap[item.categoryId];
      const catLbl = cat ? categoryLabel(cat) : (item.categoryId ? String(item.categoryId) : '—');

      const $row = $(
        '<div class="card-row" data-row="ap" data-id="' + esc(item.id) + '">' +
          '<div class="card-row-main">' +
            '<div style="width:10px;height:10px;border-radius:50%;background:' + dotColor + ';flex-shrink:0;"></div>' +
            '<div style="min-width:0;flex:1;display:flex;flex-direction:column;gap:2px;">' +
              '<span class="row-title" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-transform:uppercase;">' +
                esc(item.name || '—') +
              '</span>' +
              '<span class="row-sub">' + esc(catLbl) + '</span>' +
            '</div>' +
          '</div>' +
          '<div class="card-row-actions" style="gap:16px;">' +
            '<span style="font-size:12px;color:var(--text-muted);">' + esc(fmtDate(item.due)) + '</span>' +
            '<span class="badge badge-' + esc(badgeColor) + '">' + esc(badgeLabel) + '</span>' +
            '<span style="font-size:14px;font-weight:700;color:' + amtColor + ';min-width:100px;text-align:right;">' +
              esc(fmt(amtAbs)) +
            '</span>' +
            '<div style="position:relative;">' +
              '<button type="button" class="icon-btn" data-act="menu" data-id="' + esc(item.id) + '" ' +
                'style="width:28px;height:28px;">' +
                window.icon('moreVertical', 14) +
              '</button>' +
              '<div class="ap-menu" data-menu-for="' + esc(item.id) + '" ' +
                'style="display:none;position:absolute;right:0;top:32px;z-index:10;background:var(--bg-surface);' +
                'border:1px solid var(--border);border-radius:var(--radius-sm);min-width:170px;' +
                'box-shadow:0 8px 24px rgba(0,0,0,0.25);padding:4px;">' +
                (st !== 'confirmed'
                  ? '<button type="button" class="ap-menu-item" data-act="mark-paid" data-id="' + esc(item.id) + '">' +
                      window.icon('check', 14) + '<span>Marcar como pago</span>' +
                    '</button>'
                  : '') +
                '<button type="button" class="ap-menu-item" data-act="edit" data-id="' + esc(item.id) + '">' +
                  window.icon('edit', 14) + '<span>Editar</span>' +
                '</button>' +
                '<button type="button" class="ap-menu-item" data-act="trash" data-id="' + esc(item.id) + '" ' +
                  'style="color:var(--expense);">' +
                  window.icon('trash', 14) + '<span>Excluir</span>' +
                '</button>' +
              '</div>' +
            '</div>' +
          '</div>' +
        '</div>'
      );

      $card.append($row);
    });

    // Inject menu-item styling once.
    if (!$card.find('style[data-ap-menu]').length) {
      $card.prepend(
        '<style data-ap-menu>' +
          '.ap-menu-item{display:flex;align-items:center;gap:8px;width:100%;padding:7px 10px;' +
          'font-size:13px;font-weight:500;color:var(--text-primary);background:transparent;' +
          'border:none;border-radius:var(--radius-sm);cursor:pointer;text-align:left;' +
          'transition:background var(--transition);}' +
          '.ap-menu-item:hover{background:var(--bg-hover);}' +
        '</style>'
      );
    }

    return $card;
  }

  // ── Modal: create / edit ──────────────────────────────────
  function openFormModal(existing) {
    const isEdit = !!existing;
    const defaultType = state.tab === 'receivable' ? 'RECEIVABLE' : 'PAYABLE';
    const uniq = Date.now();
    const ids = {
      name: 'ap-name-' + uniq,
      amount: 'ap-amount-' + uniq,
      due: 'ap-due-' + uniq,
      account: 'ap-acc-' + uniq,
      category: 'ap-cat-' + uniq,
      status: 'ap-st-' + uniq,
    };

    const initial = {
      type: isEdit ? (existing.type || defaultType) : defaultType,
      name: isEdit ? (existing.name || '') : '',
      amount: isEdit ? maskCurrency(String(Math.abs(Number(existing.amount) || 0))) : '',
      due: isEdit ? String(existing.due || '').slice(0, 10) : new Date().toISOString().slice(0, 10),
      accountId: isEdit ? String(existing.accountId || '') : '',
      categoryId: isEdit ? String(existing.categoryId || '') : '',
      status: isEdit ? (existing.status || 'pending') : 'pending',
    };

    function natureForType(t) { return t === 'RECEIVABLE' ? 'INCOME' : 'EXPENSE'; }

    // keepId: mantém a categoria já gravada na lista mesmo se foi inativada depois — sem isso, editar
    // um item com categoria inativa perdia a categoria em silêncio (só reaparecia no próximo save).
    function categoryFieldHtml(t, selectedId, keepId) {
      return window.categoryPickerHtml({
        items: window.categoryItemsFor(natureForType(t), keepId),
        selectedId: selectedId,
        selectId: ids.category,
        selectAttrs: ' name="categoryId" data-region="category-select"',
        placeholder: 'Selecione',
      });
    }

    const accOpts = window.accountOptionsHtml(initial.accountId, { keepId: isEdit ? initial.accountId : null });

    const statusOpts = [
      ['pending',   'Pendente'],
      ['scheduled', 'Agendado'],
      ['confirmed', 'Confirmado'],
    ].map(function (p) {
      const sel = p[0] === initial.status ? ' selected' : '';
      return '<option value="' + esc(p[0]) + '"' + sel + '>' + esc(p[1]) + '</option>';
    }).join('');

    const TYPE_OPTIONS = [
      { value: 'PAYABLE',    label: '↓ A Pagar',   color: 'expense' },
      { value: 'RECEIVABLE', label: '↑ A Receber',  color: 'income' },
    ];

    const bodyHtml =
      '<form data-form="ap" autocomplete="off">' +
        '<div data-region="type-row" style="display:flex;gap:8px;margin-bottom:16px;">' +
          window.typeToggleHtml(TYPE_OPTIONS, initial.type) +
        '</div>' +
        '<input type="hidden" name="type" value="' + esc(initial.type) + '" />' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.name + '">Descrição</label>' +
            '<input id="' + ids.name + '" name="name" type="text" required ' +
              'style="text-transform:uppercase;" ' +
              'placeholder="Ex: Aluguel, Salário..." value="' + esc(initial.name) + '" />' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.amount + '">Valor (R$)</label>' +
            '<input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" ' +
              'placeholder="0,00" value="' + esc(initial.amount) + '" />' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.due + '">Vencimento</label>' +
            '<input id="' + ids.due + '" name="due" type="date" required value="' + esc(initial.due) + '" />' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.category + '">Categoria</label>' +
            '<div data-region="category-select-wrap">' +
              categoryFieldHtml(initial.type, initial.categoryId, initial.categoryId) +
            '</div>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.account + '">Conta</label>' +
            '<select id="' + ids.account + '" name="accountId">' + accOpts + '</select>' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.status + '">Status</label>' +
            '<select id="' + ids.status + '" name="status">' + statusOpts + '</select>' +
          '</div>' +
        '</div>' +
      '</form>';

    const m = window.formModal({
      title: isEdit ? 'Editar Conta' : 'Nova Conta',
      formName: 'ap',
      body: bodyHtml,
      onSubmit: function ($form) {
        const $amount = $form.find('input[name=amount]');
        const name = ($form.find('input[name=name]').val() || '').trim();
        if (!name) { $form.find('input[name=name]').trigger('focus'); return null; }
        const amtRaw = $amount.val();
        const amt = window.parseCurrency(amtRaw);
        if (!isFinite(amt) || amt <= 0) { $amount.trigger('focus'); return null; }
        const type = $form.find('input[name=type]').val();
        const due = $form.find('input[name=due]').val();
        const categoryId = $form.find('select[name=categoryId]').val();
        const accountId  = $form.find('select[name=accountId]').val();
        const status     = $form.find('select[name=status]').val();

        if (!accountId)  { window.toast('Selecione uma conta', 'error'); return null; }
        if (!categoryId) { window.toast('Selecione uma categoria', 'error'); return null; }

        // Persist as a transaction (payables are transactions with status pending/scheduled/confirmed).
        const txType = window.Domain.Payable.natureOf(type);
        const signed = window.Domain.Transaction.signedAmount(txType, amt);
        const payload = {
          description: name,
          amount: Number(signed.toFixed(2)),
          date: due,
          categoryId: categoryId,
          accountId: accountId,
          status: status,
          type: txType,
        };
        return isEdit
          ? window.TransactionsApi.update(existing.id, payload)
          : window.TransactionsApi.create(payload);
      },
      success: function () { return isEdit ? 'Conta atualizada' : 'Conta criada'; },
      failure: 'Falha ao salvar conta',
      onDone: loadData,
    });

    const $form = m.$body.find('form[data-form=ap]');
    const $amount = $form.find('input[name=amount]');
    if (window.bindCurrencyMask) window.bindCurrencyMask($amount);

    m.$body.on('click', '[data-act=set-form-type]', function (e) {
      e.preventDefault();
      const t = $(this).attr('data-type');
      $form.find('input[name=type]').val(t);
      m.$body.find('[data-region=type-row]').html(window.typeToggleHtml(TYPE_OPTIONS, t));
      // Categoria não sobrevive à troca de tipo (nature muda) — sem keepId, sempre reseta ao placeholder.
      m.$body.find('[data-region=category-select-wrap]').html(categoryFieldHtml(t, '', null));
    });
  }

  // Delete/mark-paid delegam para TransactionsApi (contrato público da fatia irmã transactions)
  // — ganha de brinde o escopo de parcelas (SINGLE/FUTURE/ALL) no delete.
  function openDeleteModal(item) {
    window.TransactionsApi.openDeleteFlow(item, { onDone: loadData });
  }

  function markPaid(item) {
    window.TransactionsApi.markPaid(item, { onDone: loadData });
  }

  // ── Menu toggle / outside-click close ─────────────────────
  function closeAllMenus() {
    if (!state || !state.$root) return;
    state.$root.find('.ap-menu').hide();
  }

  function bindRoot($root) {
    $root.on('click.ap', '[data-act=new]', function () { openFormModal(null); });

    $root.on('click.ap', '[data-act=menu]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const $menu = $root.find('.ap-menu[data-menu-for="' + id + '"]');
      const wasOpen = $menu.is(':visible');
      closeAllMenus();
      if (!wasOpen) $menu.show();
    });

    $root.on('click.ap', '[data-act=edit]', function (e) {
      e.stopPropagation();
      closeAllMenus();
      const item = findItem($(this).attr('data-id'));
      if (item) openFormModal(item);
    });
    $root.on('click.ap', '[data-act=trash]', function (e) {
      e.stopPropagation();
      closeAllMenus();
      const item = findItem($(this).attr('data-id'));
      if (item) openDeleteModal(item);
    });
    $root.on('click.ap', '[data-act=mark-paid]', function (e) {
      e.stopPropagation();
      closeAllMenus();
      const item = findItem($(this).attr('data-id'));
      if (item) markPaid(item);
    });

    // Outside click closes any open menu.
    $(document).on('click.ap-outside', closeAllMenus);
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['accounts-payable'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      render();
      loadData();
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.ap');
      }
      $(document).off('click.ap-outside');
      state = null;
    }
  };
})();

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
    const $header = $(
      '<div class="page-header">' +
        '<h1>A Pagar e Receber</h1>' +
        '<div class="page-header-actions" data-region="head-actions"></div>' +
      '</div>'
    );
    const $headRight = $header.find('[data-region=head-actions]');

    const $periodNav = window.periodNav({
      month: state.month + 1,
      year: state.year,
      onPrev: function () { window.shiftMonth(state, -1, false); render(); },
      onNext: function () { window.shiftMonth(state, +1, false); render(); },
      onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m - 1; state.year = y; render(); },
    });
    $headRight.append($periodNav);

    $headRight.append(
      window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Conta',
        attrs: 'data-act="new"'
      })
    );

    $page.append($header);

    // ── Summary card (3-col, divided) ──
    const periodPayable    = inPeriod(state.payable);
    const periodReceivable = inPeriod(state.receivable);
    const totalPayable     = periodPayable.reduce(function (s, i) { return s + (Number(i.amount) || 0); }, 0);
    const totalReceivable  = periodReceivable.reduce(function (s, i) { return s + (Number(i.amount) || 0); }, 0);
    const result           = totalReceivable - totalPayable;
    const resultColor      = result >= 0 ? 'var(--income)' : 'var(--expense)';

    function col(label, value, color) {
      return (
        '<div style="flex:1;">' +
          '<p style="font-size:11px;color:var(--text-muted);font-weight:600;letter-spacing:0.04em;text-transform:uppercase;">' +
            esc(label) +
          '</p>' +
          '<p style="font-size:20px;font-weight:800;color:' + color + ';margin-top:4px;">' +
            esc(fmt(value)) +
          '</p>' +
        '</div>'
      );
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
      // pending → expense dot/badge; scheduled → warning; confirmed → income.
      const dotColor   = st === 'scheduled' ? 'var(--warning)'
                     : st === 'confirmed' ? 'var(--income)'
                     : 'var(--expense)';
      const badgeColor = st === 'scheduled' ? 'warning'
                     : st === 'confirmed' ? 'income'
                     : 'expense';
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

    const accs = accountsList().filter(function (a) {
      return a.active !== false || (isEdit && String(a.id) === initial.accountId);
    });
    if (!initial.accountId && accs.length) initial.accountId = String(accs[0].id);

    function natureForType(t) { return t === 'RECEIVABLE' ? 'INCOME' : 'EXPENSE'; }

    function buildCatOptions(t, selectedId) {
      const cats = flatCategories(natureForType(t), true);
      if (!cats.length) return '<option value="">Nenhuma categoria disponível</option>';
      return cats.map(function (c) {
        const sel = String(c.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.label) + '</option>';
      }).join('');
    }

    const accOpts = accs.length
      ? accs.map(function (a) {
          const sel = String(a.id) === initial.accountId ? ' selected' : '';
          return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
        }).join('')
      : '<option value="">Nenhuma conta disponível</option>';

    const statusOpts = [
      ['pending',   'Pendente'],
      ['scheduled', 'Agendado'],
      ['confirmed', 'Confirmado'],
    ].map(function (p) {
      const sel = p[0] === initial.status ? ' selected' : '';
      return '<option value="' + esc(p[0]) + '"' + sel + '>' + esc(p[1]) + '</option>';
    }).join('');

    function typeBtnHtml(val, label, color, active) {
      return '<button type="button" data-act="set-form-type" data-type="' + esc(val) + '" ' +
        'style="flex:1;padding:8px;border-radius:var(--radius-sm);font-size:13px;font-weight:600;' +
        'border:1px solid ' + (active ? 'var(--' + color + ')' : 'var(--border)') + ';' +
        'background:' + (active ? 'var(--' + color + '-light)' : 'transparent') + ';' +
        'color:' + (active ? 'var(--' + color + ')' : 'var(--text-secondary)') + ';' +
        'cursor:pointer;transition:all var(--transition);">' +
        esc(label) +
      '</button>';
    }

    const bodyHtml =
      '<form data-form="ap" autocomplete="off">' +
        '<div data-region="type-row" style="display:flex;gap:8px;margin-bottom:16px;">' +
          typeBtnHtml('PAYABLE',    '↓ A Pagar',   'expense', initial.type === 'PAYABLE') +
          typeBtnHtml('RECEIVABLE', '↑ A Receber', 'income',  initial.type === 'RECEIVABLE') +
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
            '<select id="' + ids.category + '" name="categoryId" data-region="category-select">' +
              buildCatOptions(initial.type, initial.categoryId) +
            '</select>' +
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

    const m = window.modal({
      title: isEdit ? 'Editar Conta' : 'Nova Conta',
      body: bodyHtml,
      footer: window.saveCancelFooter(),
    });
    m.open();

    const $form = m.$body.find('form[data-form=ap]');
    const $amount = $form.find('input[name=amount]');
    if (window.bindCurrencyMask) window.bindCurrencyMask($amount);

    m.$body.on('click', '[data-act=set-form-type]', function (e) {
      e.preventDefault();
      const t = $(this).attr('data-type');
      $form.find('input[name=type]').val(t);
      const $row = m.$body.find('[data-region=type-row]');
      $row.empty();
      $row.append(typeBtnHtml('PAYABLE',    '↓ A Pagar',   'expense', t === 'PAYABLE'));
      $row.append(typeBtnHtml('RECEIVABLE', '↑ A Receber', 'income',  t === 'RECEIVABLE'));
      const $cat = m.$body.find('[data-region=category-select]');
      $cat.html(buildCatOptions(t, $cat.val()));
    });

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($form.find('input[name=name]').val() || '').trim();
      if (!name) { $form.find('input[name=name]').trigger('focus'); return; }
      const amtRaw = $form.find('input[name=amount]').val();
      const amt = window.parseCurrency(amtRaw);
      if (!isFinite(amt) || amt <= 0) { $amount.trigger('focus'); return; }
      const type = $form.find('input[name=type]').val();
      const due = $form.find('input[name=due]').val();
      const categoryId = $form.find('select[name=categoryId]').val();
      const accountId  = $form.find('select[name=accountId]').val();
      const status     = $form.find('select[name=status]').val();

      if (!accountId)  { window.toast('Selecione uma conta', 'error'); return; }
      if (!categoryId) { window.toast('Selecione uma categoria', 'error'); return; }

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

      const $btn = m.$el.find('[data-act=save]').prop('disabled', true);
      const p = isEdit
        ? window.App.TransactionService.update(existing.id, payload)
        : window.App.TransactionService.create(payload);

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Conta atualizada' : 'Conta criada', 'success');
        return loadData();
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao salvar conta', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=save]', submit);
  }

  // ── Delete ────────────────────────────────────────────────
  function openDeleteModal(item) {
    window.confirmModal({
      title: 'Excluir Conta',
      body: window.modalText('Excluir <strong>' + esc(item.name || 'conta') + '</strong>? Esta ação não pode ser desfeita.'),
      onConfirm: function (m, reEnable) {
        window.App.TransactionService.remove(item.accountId, item.id).then(function () {
          m.close();
          window.toast('Conta excluída', 'success');
          return loadData();
        }).catch(function (err) {
          reEnable();
          window.toast((err && err.message) || 'Falha ao excluir', 'error');
        });
      },
    });
  }

  // ── Mark as paid ──────────────────────────────────────────
  function markPaid(item) {
    const today = new Date().toISOString().slice(0, 10);
    window.App.TransactionService.patchStatus(item.accountId, item.id, 'confirmed', today).then(function () {
      window.toast('Conta confirmada', 'success');
      return loadData();
    }).catch(function (err) {
      window.toast((err && err.message) || 'Falha ao confirmar', 'error');
    });
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

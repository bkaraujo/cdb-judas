/* pages/transactions.js — Lançamentos (list + filtros + CRUD modal + status). */
(function () {
  window.Pages = window.Pages || {};

  // ── Constants ─────────────────────────────────────────────
  const STATUS_LABEL = {
    confirmed: 'Confirmado',
    pending: 'Pendente',
    scheduled: 'Agendado',
    planed: 'Planejado',
    balance: 'Saldo'
  };
  // Per task spec: pending=expense, scheduled=warning, confirmed=income.
  const STATUS_BADGE = {
    confirmed: 'income',
    pending: 'expense',
    scheduled: 'warning',
    planed: 'info',
    balance: 'muted'
  };

  // ── State ─────────────────────────────────────────────────
  let state = null;

  function resetState() {
    const p = window.App.PeriodService.get(); // { month: 1-12, year }
    state = {
      $root: null,
      loading: true,
      transactions: [],
      month: p.month - 1,       // 0-11
      year: p.year,
      search: '',
      filterType: 'all',           // 'all'|'income'|'expense'|'transfer'
      filterAccount: '',
      filterCategory: '',
      filterStatus: '',
      showFilters: false,
      openMenuId: null,            // currently-open row dropdown
    };
  }

  // ── Helpers ───────────────────────────────────────────────

  function findTx(id) { return window.byId(state.transactions, id); }

  // Default date for a new transaction: today when the displayed month is the
  // current month, otherwise the first day of the month being displayed.
  function defaultNewDate() {
    const now = new Date();
    if (state.month === now.getMonth() && state.year === now.getFullYear()) {
      return now.toISOString().slice(0, 10);
    }
    return window.monthBounds(state.month, state.year).from;
  }

  // Row actions (create/edit modal, delete, mark-paid) live in the shared
  // pages/transactions/actions.js; state-dependent bits (period tx list for transfer
  // detection, default date, reload) are passed through.
  function openFormModal(existing) {
    window.transactionActions.openFormModal({
      existing: existing || null,
      list: state.transactions,
      defaultDate: defaultNewDate(),
      onSaved: function () { return loadTransactions(); },
    });
  }

  // ── Fetch ─────────────────────────────────────────────────
  function loadTransactions() {
    state.loading = true;
    render();
    const b = monthBounds(state.month, state.year);
    const qs = 'dateFrom=' + b.from + '&dateTo=' + b.to;
    return window.App.TransactionService.list(qs).then(function (list) {
      state.transactions = Array.isArray(list) ? list : [];
      state.loading = false;
      render();
    }).catch(function (err) {
      state.loading = false;
      state.transactions = [];
      render();
      window.toast((err && err.message) || 'Falha ao carregar lançamentos', 'error');
    });
  }

  // ── Filtering / Summary ───────────────────────────────────
  function filteredTxs() {
    const catMap = categoryById();
    const s = (state.search || '').toLowerCase();
    return state.transactions.filter(function (tx) {
      const cat = catMap[tx.categoryId];
      const catLbl = cat ? categoryLabel(cat).toLowerCase() : '';
      const matchSearch = !s ||
        (tx.description || '').toLowerCase().indexOf(s) >= 0 ||
        catLbl.indexOf(s) >= 0;
      const matchType = state.filterType === 'all' || tx.type === state.filterType;
      const matchAcc  = !state.filterAccount || String(tx.accountId) === String(state.filterAccount);
      const matchCat  = !state.filterCategory || String(tx.categoryId) === String(state.filterCategory);
      const matchSt   = !state.filterStatus || tx.status === state.filterStatus;
      return matchSearch && matchType && matchAcc && matchCat && matchSt;
    });
  }

  function summary(list) {
    let income = 0, expense = 0;
    list.forEach(function (t) {
      if (t.type === 'income') income += Number(t.amount) || 0;
      else if (t.type === 'expense') expense += Math.abs(Number(t.amount) || 0);
    });
    return { income: income, expense: expense, result: income - expense };
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $page = $('<div class="fade-in"></div>');

    // ── Page header ──
    const $header = $(
      '<div class="page-header">' +
        '<h1>Lançamentos</h1>' +
        '<div class="page-header-actions" data-region="head-actions"></div>' +
      '</div>'
    );
    const $headRight = $header.find('[data-region=head-actions]');

    // period nav
    const $periodNav = window.periodNav({
      month: state.month + 1,
      year: state.year,
      onPrev: function () { window.shiftMonth(state, -1, false); loadTransactions(); },
      onNext: function () { window.shiftMonth(state, +1, false); loadTransactions(); },
      onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m - 1; state.year = y; loadTransactions(); },
    });
    $headRight.append($periodNav);

    // Filter toggle
    $headRight.append(
      window.btn({
        variant: state.showFilters ? 'primary' : 'secondary',
        size: 'md', icon: 'filter',
        label: 'Filtros',
        attrs: 'data-act="toggle-filters"'
      })
    );

    // Importar
    $headRight.append(
      window.btn({
        variant: 'secondary', size: 'md', icon: 'download', label: 'Importar',
        attrs: 'data-act="import"'
      })
    );

    // Novo Lançamento
    $headRight.append(
      window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Novo Lançamento',
        attrs: 'data-act="new"'
      })
    );

    $page.append($header);

    // ── Summary cards ──
    const sum = summary(filteredTxs());
    const resultColor = sum.result >= 0 ? 'var(--income)' : 'var(--expense)';
    const $summary = $(
      '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:18px;">' +
        summaryCardHtml('arrowUp',   'RECEITAS',  fmt(sum.income),  'var(--income)') +
        summaryCardHtml('arrowDown', 'DESPESAS',  fmt(sum.expense), 'var(--expense)') +
        summaryCardHtml('trendingUp','RESULTADO', fmt(sum.result),  resultColor) +
      '</div>'
    );
    $page.append($summary);

    // ── Quick filter / search row ──
    const $quick = $(
      '<div style="display:flex;gap:10px;align-items:center;margin-bottom:14px;flex-wrap:wrap;">' +
        '<div style="position:relative;flex:1;max-width:320px;min-width:200px;">' +
          '<div style="position:absolute;left:10px;top:50%;transform:translateY(-50%);color:var(--text-muted);display:flex;">' +
            window.icon('search', 15) +
          '</div>' +
          '<input data-act="search" type="text" placeholder="Pesquisar lançamentos..." ' +
            'value="' + esc(state.search) + '" style="padding-left:32px;" />' +
        '</div>' +
        chipBtn('all', 'Todos') +
        chipBtn('income', 'Receitas') +
        chipBtn('expense', 'Despesas') +
        chipBtn('transfer', 'Transferências') +
      '</div>'
    );
    $page.append($quick);

    // ── Advanced filters ──
    if (state.showFilters) {
      $page.append(renderAdvancedFilters());
    }

    // ── Table / list ──
    $page.append(renderList());

    $root.empty().append($page);
  }

  function summaryCardHtml(iconName, label, value, color) {
    return (
      '<div class="card" style="padding:14px 18px;display:flex;align-items:center;gap:12px;">' +
        '<span style="color:' + color + ';display:flex;">' + window.icon(iconName, 20) + '</span>' +
        '<div>' +
          '<p style="font-size:11px;color:var(--text-muted);font-weight:600;letter-spacing:0.04em;">' +
            esc(label) +
          '</p>' +
          '<p style="font-size:18px;font-weight:800;color:' + color + ';margin-top:2px;">' +
            esc(value) +
          '</p>' +
        '</div>' +
      '</div>'
    );
  }

  function chipBtn(val, label) {
    const active = state.filterType === val;
    const style =
      'padding:6px 14px;border-radius:var(--radius-sm);font-size:13px;font-weight:500;' +
      'border:1px solid var(--border);cursor:pointer;transition:all var(--transition);' +
      (active
        ? 'background:var(--accent);color:#fff;'
        : 'background:transparent;color:var(--text-secondary);');
    return '<button type="button" data-act="set-type" data-type="' + esc(val) + '" ' +
      'style="' + style + '">' + esc(label) + '</button>';
  }

  function renderAdvancedFilters() {
    const accs = accountsList();
    const cats = flatCategories(null);

    const accOpts = '<option value="">Todas</option>' + accs.map(function (a) {
      const sel = String(a.id) === String(state.filterAccount) ? ' selected' : '';
      return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
    }).join('');

    const catOpts = '<option value="">Todas</option>' + cats.map(function (c) {
      const sel = String(c.id) === String(state.filterCategory) ? ' selected' : '';
      return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.label) + '</option>';
    }).join('');

    const stOpts = ['', 'confirmed', 'pending', 'scheduled', 'planed'].map(function (st) {
      const lbl = st === '' ? 'Todos' : (STATUS_LABEL[st] || st);
      const sel = st === state.filterStatus ? ' selected' : '';
      return '<option value="' + esc(st) + '"' + sel + '>' + esc(lbl) + '</option>';
    }).join('');

    const hasAny = !!(state.filterAccount || state.filterCategory || state.filterStatus);

    return $(
      '<div class="card" style="padding:16px;margin-bottom:14px;">' +
        '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;">' +
          '<div class="form-group">' +
            '<label class="form-label">Conta</label>' +
            '<select data-act="filter-account">' + accOpts + '</select>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label">Categoria</label>' +
            '<select data-act="filter-category">' + catOpts + '</select>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label">Status</label>' +
            '<select data-act="filter-status">' + stOpts + '</select>' +
          '</div>' +
        '</div>' +
        (hasAny
          ? '<div style="display:flex;justify-content:flex-end;margin-top:10px;">' +
              '<button type="button" data-act="clear-filters" class="btn btn-ghost btn-sm">' +
                window.icon('x', 13) + '<span>Limpar filtros</span>' +
              '</button>' +
            '</div>'
          : ''
        ) +
      '</div>'
    );
  }

  function renderList() {
    const list = filteredTxs();

    const $card = $('<div class="card" style="padding:0;overflow:hidden;"></div>');

    if (state.loading) {
      $card.append(window.emptyState({ icon: 'list', title: 'Carregando…' }));
      return $card;
    }
    if (list.length === 0) {
      $card.append(window.emptyState({
        icon: 'list',
        title: 'Nenhum lançamento encontrado',
        desc: 'Ajuste os filtros ou crie um novo lançamento.'
      }));
      return $card;
    }

    const catMap = categoryById();
    const accs = window.App.CacheStore.accounts();
    const accMap = {};
    accs.forEach(function (a) { accMap[String(a.id)] = a; });

    // Sort by date desc.
    const sorted = list.slice().sort(function (a, b) {
      return String(b.date).localeCompare(String(a.date));
    });

    sorted.forEach(function (tx, idx) {
      const isLast = idx === sorted.length - 1;
      const cat = catMap[tx.categoryId];
      const catLbl = cat ? categoryLabel(cat) : '—';
      const acc = accMap[String(tx.accountId)];
      const accName = acc ? acc.name : '—';
      const amt = Number(tx.amount) || 0;
      const amtColor =
        tx.type === 'income' ? 'var(--income)' :
        tx.type === 'transfer' ? 'var(--text-secondary)' :
        'var(--expense)';
      const stKey = tx.status || 'confirmed';

      const iconName = tx.type === 'income' ? 'arrowUp'
                   : tx.type === 'transfer' ? 'activity'
                   : 'arrowDown';

      const $row = $(
        '<div class="card-row" data-row="tx" data-id="' + esc(tx.id) + '" ' +
          'style="padding: 8px 16px;' + (isLast ? 'border-bottom:none;' : '') + '">' +
          '<div class="card-row-main">' +
            '<div style="width:30px;height:30px;border-radius:8px;flex-shrink:0;display:flex;align-items:center;justify-content:center;' +
              'background:' + (tx.type === 'income' ? 'var(--income-light)' : tx.type === 'transfer' ? 'var(--accent-light)' : 'var(--expense-light)') + ';' +
              'color:' + amtColor + ';">' +
              window.icon(iconName, 14) +
            '</div>' +
            '<div style="min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px;">' +
              '<span style="color:var(--text-muted);">' + esc(fmtDate(tx.date)) + '</span>' +
              '<span style="color:var(--text-muted);"> • </span>' +
              '<span style="font-weight:600;text-transform:uppercase;">' + esc(tx.description || '—') + '</span>' +
              '<span style="color:var(--text-muted);"> • ' + esc(accName) + ' • ' + esc(catLbl) + '</span>' +
            '</div>' +
            '<span class="badge badge-' + esc(STATUS_BADGE[stKey] || 'muted') + '" ' +
              'style="flex-shrink:0;">' + esc(STATUS_LABEL[stKey] || stKey) + '</span>' +
            '<div style="min-width:120px;text-align:right;font-weight:700;font-size:14px;color:' + amtColor + ';flex-shrink:0;">' +
              esc(fmt(amt)) +
            '</div>' +
          '</div>' +
          '<div class="card-row-actions" data-region="row-actions"></div>' +
        '</div>'
      );

      const $acts = $row.find('[data-region=row-actions]');
      // Quick mark-as-paid for non-confirmed.
      if (stKey !== 'confirmed') {
        $acts.append(
          '<button type="button" class="icon-btn" title="Confirmar" ' +
            'data-act="mark-paid" data-id="' + esc(tx.id) + '" ' +
            'style="width:28px;height:28px;color:var(--income);">' +
            window.icon('check', 14) +
          '</button>'
        );
      }
      $acts.append(
        '<button type="button" class="icon-btn" title="Editar" ' +
          'data-act="edit" data-id="' + esc(tx.id) + '" ' +
          'style="width:28px;height:28px;">' +
          window.icon('edit', 14) +
        '</button>'
      );
      $acts.append(
        '<button type="button" class="icon-btn" title="Excluir" ' +
          'data-act="trash" data-id="' + esc(tx.id) + '" ' +
          'style="width:28px;height:28px;color:var(--expense);">' +
          window.icon('trash', 14) +
        '</button>'
      );

      $card.append($row);
    });

    return $card;
  }

  // ── Event delegation ──────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.tx', '[data-act=new]', function () { openFormModal(null); });
    $root.on('click.tx', '[data-act=import]', function () {
      window.importStatementModal({ onImported: function () { return loadTransactions(); } });
    });

    $root.on('click.tx', '[data-act=toggle-filters]', function () {
      state.showFilters = !state.showFilters;
      render();
    });

    $root.on('click.tx', '[data-act=set-type]', function () {
      const t = $(this).attr('data-type');
      if (t && t !== state.filterType) {
        state.filterType = t;
        render();
      }
    });

    $root.on('input.tx', '[data-act=search]', function () {
      state.search = $(this).val() || '';
      // Re-render only the list+summary area would be ideal; full render is fine for now.
      // Preserve focus by refocusing the search input after render.
      const caretPos = this.selectionStart;
      render();
      const $input = state.$root.find('[data-act=search]');
      $input.trigger('focus');
      try { $input[0].setSelectionRange(caretPos, caretPos); } catch (e) { /* noop */ }
    });

    $root.on('change.tx', '[data-act=filter-account]', function () {
      state.filterAccount = $(this).val() || '';
      render();
    });
    $root.on('change.tx', '[data-act=filter-category]', function () {
      state.filterCategory = $(this).val() || '';
      render();
    });
    $root.on('change.tx', '[data-act=filter-status]', function () {
      state.filterStatus = $(this).val() || '';
      render();
    });
    $root.on('click.tx', '[data-act=clear-filters]', function () {
      state.filterAccount = '';
      state.filterCategory = '';
      state.filterStatus = '';
      render();
    });

    $root.on('click.tx', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const tx = findTx($(this).attr('data-id'));
      if (tx) openFormModal(tx);
    });
    $root.on('click.tx', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const tx = findTx($(this).attr('data-id'));
      if (tx) window.transactionActions.openDeleteModal(tx, { list: state.transactions, onDone: loadTransactions });
    });
    $root.on('click.tx', '[data-act=mark-paid]', function (e) {
      e.stopPropagation();
      const tx = findTx($(this).attr('data-id'));
      if (tx) window.transactionActions.markPaid(tx, { onDone: loadTransactions });
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['transactions'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      render();
      loadTransactions();
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.tx');
      }
      state = null;
    }
  };
})();

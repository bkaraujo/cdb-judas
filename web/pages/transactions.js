/* pages/transactions.js — Lançamentos (list + filtros + CRUD modal + status).
 * Card da lista no leiaute de linha do Extrato de Contas: data | conta (coluna fixa) |
 * categoria (coluna fixa) | descrição | badge de status | valor | ações. */
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

    // Footer: how many transactions are on display.
    if (!state.loading) {
      const txCount = filteredTxs().length;
      $page.append(
        '<div style="text-align:right;padding:12px 4px 0;font-size:12px;color:var(--text-muted);">' +
          esc(txCount + (txCount === 1 ? ' transação exibida' : ' transações exibidas')) +
        '</div>'
      );
    }

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

  // Statement-style card: data | conta (coluna fixa) | categoria (coluna fixa) | descrição | badge | valor | ações.
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

    // Fixed category column: width of the widest possible category label so
    // every description starts at the same x, regardless of each row's category.
    const catLens = window.flatCategories().map(function (c) { return c.label.length; });
    const catColCh = (catLens.length ? Math.max.apply(null, catLens) : 12) + 1;

    // Fixed account column, same idea (widest account name).
    const accLens = accs.map(function (a) { return (a.name || '').length; });
    const accColCh = (accLens.length ? Math.max.apply(null, accLens) : 10) + 1;

    // Sort by date desc.
    const sorted = list.slice().sort(function (a, b) {
      return String(b.date).localeCompare(String(a.date));
    });

    sorted.forEach(function (tx, i) {
      const isLast = i === sorted.length - 1;
      const cat = catMap[tx.categoryId];
      const catLbl = cat ? categoryLabel(cat) : '';
      const acc = accMap[String(tx.accountId)];
      const accName = acc ? acc.name : '—';
      const amt = Number(tx.amount) || 0;
      const amtColor =
        tx.type === 'income' ? 'var(--income)' :
        tx.type === 'transfer' ? 'var(--text-secondary)' :
        'var(--expense)';
      const stKey = tx.status || 'confirmed';

      const rowStyle =
        'display:flex;align-items:center;gap:16px;padding:11px 20px;' +
        (isLast ? '' : 'border-bottom:1px solid var(--border-light);') +
        'transition:background var(--transition);';

      const descStyle =
        'flex:1;font-size:13px;font-weight:500;color:var(--text-primary);' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-transform:uppercase;';

      const catStyle =
        'flex:0 0 ' + catColCh + 'ch;width:' + catColCh + 'ch;' +
        'font-size:12px;color:var(--text-muted);' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

      const accStyle =
        'flex:0 0 ' + accColCh + 'ch;width:' + accColCh + 'ch;' +
        'font-size:12px;color:var(--text-muted);text-align:left;' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

      // Row actions; mark-paid only for non-confirmed rows.
      const markPaidHtml = stKey !== 'confirmed'
        ? '<button type="button" class="icon-btn" title="Confirmar" ' +
            'data-act="mark-paid" data-id="' + esc(tx.id) + '" ' +
            'style="width:28px;height:28px;color:var(--income);">' +
            window.icon('check', 14) +
          '</button>'
        : '';
      const actionsHtml =
        markPaidHtml +
        '<button type="button" class="icon-btn" title="Editar" ' +
          'data-act="edit" data-id="' + esc(tx.id) + '" style="width:28px;height:28px;">' +
          window.icon('edit', 14) +
        '</button>' +
        '<button type="button" class="icon-btn" title="Excluir" ' +
          'data-act="trash" data-id="' + esc(tx.id) + '" style="width:28px;height:28px;color:var(--expense);">' +
          window.icon('trash', 14) +
        '</button>';

      $card.append(
        '<div class="stm-row" data-row="tx" data-id="' + esc(tx.id) + '" style="' + rowStyle + '">' +
          '<span style="font-size:12px;color:var(--text-muted);min-width:56px;">' +
            esc(fmtDate(tx.date)) +
          '</span>' +
          '<span style="' + accStyle + '">' + esc(accName) + '</span>' +
          '<span style="' + catStyle + '">' + esc(catLbl) + '</span>' +
          '<span style="' + descStyle + '">' + esc(tx.description || '—') + '</span>' +
          '<span class="badge badge-' + esc(STATUS_BADGE[stKey] || 'muted') + '" ' +
          'style="flex-shrink:0;">' + esc(STATUS_LABEL[stKey] || stKey) + '</span>' +
          '<span style="font-size:13px;font-weight:700;color:' + amtColor + ';min-width:100px;text-align:right;">' +
            esc(fmt(amt)) +
          '</span>' +
          '<div style="display:flex;align-items:center;justify-content:flex-end;gap:2px;width:40px;flex-shrink:0;">' + actionsHtml + '</div>' +
        '</div>'
      );
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

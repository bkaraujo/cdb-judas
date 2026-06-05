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
    const now = new Date();
    state = {
      $root: null,
      loading: true,
      transactions: [],
      month: now.getMonth(),       // 0-11
      year: now.getFullYear(),
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

  function findTx(id) {
    for (let i = 0; i < state.transactions.length; i++) {
      if (String(state.transactions[i].id) === String(id)) return state.transactions[i];
    }
    return null;
  }

  // A transfer is stored as two legs (one income + one expense) sharing a groupId, unlike
  // installments whose legs share a single type. Both legs carry the same date, so they sit in
  // the same month view together — detection works off the loaded list.
  function isTransferTx(tx) {
    if (!tx || !tx.groupId) return false;
    const group = state.transactions.filter(function (t) { return String(t.groupId) === String(tx.groupId); });
    const hasIncome = group.some(function (t) { return t.type === 'income'; });
    const hasExpense = group.some(function (t) { return t.type === 'expense'; });
    return hasIncome && hasExpense;
  }

  // Default date for a new transaction: today when the displayed month is the
  // current month, otherwise the first day of the month being displayed.
  function defaultNewDate() {
    const now = new Date();
    if (state.month === now.getMonth() && state.year === now.getFullYear()) {
      return now.toISOString().slice(0, 10);
    }
    return window.monthBounds(state.month, state.year).from;
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
      label: window.monthLabel(state.month + 1, state.year),
      onPrev: function () {
        if (--state.month < 0) { state.month = 11; state.year--; }
        loadTransactions();
      },
      onNext: function () {
        if (++state.month > 11) { state.month = 0; state.year++; }
        loadTransactions();
      },
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
          (isLast ? 'style="border-bottom:none;"' : '') + '>' +
          '<div class="card-row-main">' +
            '<div style="width:34px;height:34px;border-radius:8px;flex-shrink:0;display:flex;align-items:center;justify-content:center;' +
              'background:' + (tx.type === 'income' ? 'var(--income-light)' : tx.type === 'transfer' ? 'var(--accent-light)' : 'var(--expense-light)') + ';' +
              'color:' + amtColor + ';">' +
              window.icon(iconName, 16) +
            '</div>' +
            '<div style="min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px;">' +
              '<span style="color:var(--text-muted);">' + esc(fmtDate(tx.date)) + '</span>' +
              '<span style="color:var(--text-muted);"> • </span>' +
              '<span style="font-weight:600;">' + esc(tx.description || '—') + '</span>' +
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

  // ── Modal: import fatura (PDF) ────────────────────────────
  function openImportModal() {
    const uniq = Date.now();
    const fileId = 'import-file-' + uniq;
    const pwdId = 'import-pwd-' + uniq;
    const cardSelectId = 'import-card-' + uniq;

    const bodyHtml =
      '<div class="form-group">' +
        '<label class="form-label" for="' + fileId + '">Selecionar fatura (PDF)</label>' +
        '<input id="' + fileId + '" type="file" accept=".pdf,application/pdf" ' +
          'style="padding: 24px; border: 2px dashed var(--border); border-radius: var(--radius); ' +
          'background: var(--bg-hover); color: var(--text-primary); cursor: pointer; text-align: center; width: 100%;" />' +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:8px;">' +
          'Envie o PDF original: fatura do cartão (Santander ou BTG) ou extrato da conta corrente (BTG). ' +
          'O tipo é detectado automaticamente.' +
        '</p>' +
        '<button type="button" data-act="toggle-password" ' +
          'style="margin-top:6px;background:none;border:none;color:var(--accent);cursor:pointer;font-size:12px;padding:0;">' +
          'PDF protegido? Informar senha' +
        '</button>' +
      '</div>' +
      '<div class="form-group" data-region="password-block" style="display:none;">' +
        '<label class="form-label" for="' + pwdId + '">Senha do PDF</label>' +
        '<input id="' + pwdId + '" type="password" autocomplete="off" placeholder="Senha da fatura" />' +
        '<p data-region="password-hint" style="font-size:12px;color:var(--expense);margin-top:6px;display:none;"></p>' +
      '</div>';

    const $cancel = window.btn({
      variant: 'ghost', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $import = window.btn({
      variant: 'primary', size: 'md', icon: 'download', label: 'Importar',
      attrs: 'data-act="do-import" type="button"'
    });
    const $footer = $('<div style="display:flex;gap:10px;"></div>').append($cancel).append($import);

    const m = window.modal({
      title: 'Importar Fatura ou Extrato',
      body: bodyHtml,
      footer: $footer[0].outerHTML,
      persistent: true
    });
    m.open();
    m.$el.find('.modal-box').css({ width: '70vw', 'min-width': '560px' });

    let selectedFile = null;
    let selectedCardId = null;
    let selectedAccountId = null;
    let previewData = null;
    let statementData = null;
    let lastPassword = null;
    let sortCol = null;
    let sortAsc = true;
    const $pwdBlock = m.$el.find('[data-region=password-block]');
    const $pwd = m.$el.find('#' + pwdId);
    const $pwdHint = m.$el.find('[data-region=password-hint]');

    m.$el.find('#' + fileId).on('change', function (e) {
      selectedFile = (e.target.files && e.target.files[0]) || null;
    });

    function revealPassword(message) {
      $pwdBlock.show();
      if (message) $pwdHint.text(message).show();
      $pwd.trigger('focus');
    }

    function fmtIsoDate(iso) {
      const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(iso || ''));
      return m ? (m[3] + '/' + m[2] + '/' + m[1]) : (iso || '');
    }

    function statusTag(status) {
      const scheduled = status === 'scheduled';
      const label = scheduled ? 'Agendado' : 'Confirmado';
      const color = scheduled ? 'var(--text-muted)' : 'var(--income)';
      return '<span style="font-size:11px;color:' + color + ';">' + esc(label) + '</span>';
    }

    function importCategories() {
      const expense = flatCategories('EXPENSE', true);
      return expense.length ? expense : flatCategories(null, true);
    }

    // Import category set for a given movement type (statement rows can be income or expense).
    function importCategoriesFor(type) {
      const nature = type === 'income' ? 'REVENUE' : 'EXPENSE';
      const byNature = flatCategories(nature, true);
      return byNature.length ? byNature : flatCategories(null, true);
    }

    function categorySelectHtml(selectedId, idx) {
      const cats = importCategories();
      if (!cats.length) {
        return '<select data-row-category data-idx="' + idx + '" disabled>' +
          '<option value="">Sem categorias</option></select>';
      }
      const options = cats.map(function (c) {
        const sel = String(c.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.label) + '</option>';
      }).join('');
      return '<select data-row-category data-idx="' + idx + '" ' +
        'style="width:auto;font-size:12px;padding:4px 6px;">' + options + '</select>';
    }

    function previewRowHtml(row, idx) {
      const hasParcela = row.installmentNumber != null && row.installmentTotal != null;
      const parcela = hasParcela ? esc(row.installmentNumber + '/' + row.installmentTotal) : '—';
      const dup = !!row.duplicate;
      const dupTag = dup
        ? ' <span style="color:var(--text-muted);font-size:11px;font-style:italic;">já importado</span>'
        : '';
      const checked = row.checked !== undefined ? (row.checked ? 'checked' : '') : (dup ? '' : 'checked');
      return (
        '<tr style="border-top:1px solid var(--border);' + (dup ? 'opacity:0.6;' : '') + '">' +
          '<td style="padding:8px 10px;text-align:center;">' +
            '<input type="checkbox" data-row-include data-idx="' + idx + '" ' + checked + ' ' +
              'style="width:16px;height:16px;cursor:pointer;" />' +
          '</td>' +
          '<td style="padding:8px 10px;width:90px;white-space:nowrap;color:var(--text-secondary);">' + esc(fmtIsoDate(row.date)) + '</td>' +
          '<td style="padding:8px 10px;">' + categorySelectHtml(row.categoryId, idx) + '</td>' +
          '<td style="padding:8px 10px;text-align:center;color:var(--text-secondary);">' + parcela + '</td>' +
          '<td style="padding:8px 10px;">' +
            '<input type="text" data-row-description data-idx="' + idx + '" value="' + esc(row.description) + '" ' +
              'style="width:100%;font-size:12px;padding:4px 6px;border:1px solid transparent;background:transparent;color:inherit;outline:none;" ' +
              'onfocus="this.style.border=\'1px solid var(--border)\';this.style.background=\'var(--bg-card)\'" ' +
              'onblur="this.style.border=\'1px solid transparent\';this.style.background=\'transparent\'" />' +
            (row.last4 ? ' <span style="color:var(--text-muted);font-size:11px;">•' + esc(row.last4) + '</span>' : '') +
            dupTag +
          '</td>' +
          '<td style="padding:8px 10px;text-align:center;white-space:nowrap;">' + statusTag(row.status) + '</td>' +
          '<td style="padding:8px 10px;text-align:right;font-weight:700;white-space:nowrap;">' + esc(fmt(row.amount)) + '</td>' +
        '</tr>'
      );
    }

    function showPreview(preview) {
      previewData = preview;
      const issuer = (preview && preview.issuer) || 'UNKNOWN';
      const label = issuer === 'SANTANDER' ? 'Santander'
                  : issuer === 'BTG' ? 'BTG Pactual'
                  : issuer;
      const last4s = (preview && preview.last4s) || [];
      const rows = (preview && preview.rows) || [];

      const candidateCards = (preview && preview.candidateCards) || [];
      const matchedCardId = (preview && preview.matchedCardId) || null;
      selectedCardId = matchedCardId;

      const cardsLine = last4s.length
        ? '<p style="font-size:12px;color:var(--text-muted);margin-top:2px;">' +
            'Cartões: ' + esc(last4s.map(function (l) { return '•••• ' + l; }).join('  ')) +
          '</p>'
        : '';

      function cardOptionLabel(card) {
        return card.name + (card.last4 ? ' — •••• ' + card.last4 : '');
      }

      const matchedCard = candidateCards.filter(function (c) { return c.id === matchedCardId; })[0] || null;
      const cardHeadline = matchedCard
        ? 'Cartão detectado: ' + matchedCard.name
        : 'Cartão não identificado — selecione';

      const optionsHtml = (matchedCardId
        ? ''
        : '<option value="" selected>Selecione o cartão</option>') +
        candidateCards.map(function (card) {
          const sel = card.id === matchedCardId ? ' selected' : '';
          return '<option value="' + esc(card.id) + '"' + sel + '>' + esc(cardOptionLabel(card)) + '</option>';
        }).join('');

      const cardSelectorHtml = candidateCards.length
        ? '<div class="form-group" style="margin-top:12px;">' +
            '<label class="form-label" for="' + cardSelectId + '">' + esc(cardHeadline) + '</label>' +
            '<select id="' + cardSelectId + '" data-region="card-select">' + optionsHtml + '</select>' +
          '</div>'
        : '';

      function sortIcon(col) {
        if (sortCol !== col) return '';
        return sortAsc ? ' ↑' : ' ↓';
      }

      const tableRows = rows.map(previewRowHtml).join('');
      const tableHtml = rows.length
        ? '<div style="max-height:360px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius-sm);margin-top:12px;">' +
            '<table style="width:100%;border-collapse:collapse;font-size:13px;">' +
              '<thead><tr style="position:sticky;top:0;background:var(--bg-hover);">' +
                '<th style="padding:8px 10px;width:34px;text-align:center;">' +
                  '<input type="checkbox" data-act="select-all" checked style="width:16px;height:16px;cursor:pointer;" />' +
                '</th>' +
                '<th data-sort="date" style="padding:8px 10px;width:90px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Data' + sortIcon('date') + '</th>' +
                '<th data-sort="categoryId" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Categoria' + sortIcon('categoryId') + '</th>' +
                '<th data-sort="installmentNumber" style="padding:8px 10px;text-align:center;font-size:11px;color:var(--text-muted);cursor:pointer;">Parcela' + sortIcon('installmentNumber') + '</th>' +
                '<th data-sort="description" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Descrição' + sortIcon('description') + '</th>' +
                '<th data-sort="status" style="padding:8px 10px;text-align:center;font-size:11px;color:var(--text-muted);cursor:pointer;">Status' + sortIcon('status') + '</th>' +
                '<th data-sort="amount" style="padding:8px 10px;text-align:right;font-size:11px;color:var(--text-muted);cursor:pointer;">Valor' + sortIcon('amount') + '</th>' +
              '</tr></thead>' +
              '<tbody>' + tableRows + '</tbody>' +
            '</table>' +
          '</div>'
        : '<p style="font-size:13px;color:var(--text-muted);margin-top:12px;">Nenhum lançamento encontrado na fatura.</p>';

      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + window.icon('check', 22) + '</span>' +
          '<div>' +
            '<p style="font-size:13px;color:var(--text-muted);">Banco detectado</p>' +
            '<p style="font-size:18px;font-weight:800;">' + esc(label) + '</p>' +
            cardsLine +
          '</div>' +
        '</div>' +
        cardSelectorHtml +
        tableHtml +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:10px;">' +
          esc(rows.length + ' lançamento(s) encontrado(s). Revise, ajuste as categorias e confirme a importação.') +
        '</p>'
      );

      m.$el.find('[data-region=card-select]').on('change', function () {
        selectedCardId = this.value || null;
      });

      // Swap the upload action for a confirm action (only when there are rows to import).
      m.$el.find('[data-act=do-import]').hide();
      const $footer = m.$el.find('.modal-footer');
      $footer.find('[data-act=do-confirm]').remove();
      if (rows.length) {
        $footer.append(window.btn({
          variant: 'primary', size: 'md', icon: 'check', label: 'Confirmar importação',
          attrs: 'data-act="do-confirm" type="button"'
        }));
      }
    }

    function confirmImport() {
      const preview = previewData;
      if (!preview) return;
      if (!selectedCardId) { window.toast('Selecione o cartão de destino', 'error'); return; }

      const rows = [];
      m.$el.find('[data-row-include]').each(function () {
        if (!this.checked) return;
        const idx = Number($(this).attr('data-idx'));
        const src = preview.rows && preview.rows[idx];
        if (!src) return;
        const $cat = m.$el.find('[data-row-category][data-idx="' + idx + '"]');
        const $desc = m.$el.find('[data-row-description][data-idx="' + idx + '"]');
        const categoryId = ($cat.val() || src.categoryId) || null;
        const description = ($desc.val() || src.description || '').trim();
        rows.push({
          description: description,
          amount: src.amount,
          date: src.date,
          originalDate: src.originalDate,
          installmentNumber: src.installmentNumber,
          installmentTotal: src.installmentTotal,
          categoryId: categoryId,
        });
      });

      if (!rows.length) { window.toast('Selecione ao menos um lançamento', 'error'); return; }

      const $btn = m.$el.find('[data-act=do-confirm]').prop('disabled', true);
      window.App.TransactionService.importConfirm({ cardId: selectedCardId, rows: rows })
        .then(function (res) {
          const created = (res && res.created) || 0;
          const skipped = (res && res.skipped) || 0;
          showConfirmSummary(created, skipped);
          return loadTransactions();
        })
        .catch(function (err) {
          $btn.prop('disabled', false);
          window.toast((err && err.message) || 'Falha ao confirmar a importação', 'error');
        });
    }

    function showConfirmSummary(created, skipped, reconciled) {
      const parts = [created + ' criados'];
      if (reconciled) parts.push(reconciled + ' conciliados');
      parts.push(skipped + ' ignorados');
      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + window.icon('check', 22) + '</span>' +
          '<div>' +
            '<p style="font-size:18px;font-weight:800;">Importação concluída</p>' +
            '<p style="font-size:13px;color:var(--text-muted);margin-top:2px;">' +
              esc(parts.join(', ')) +
            '</p>' +
          '</div>' +
        '</div>'
      );
      const $footer = m.$el.find('.modal-footer');
      $footer.empty().append(window.btn({
        variant: 'primary', size: 'md', label: 'Fechar',
        attrs: 'data-modal-close="1" type="button"'
      }));
    }

    // ── Bank statement (extrato) preview ───────────────────────
    function routePreview(preview) {
      if (preview && preview.documentType === 'BANK_STATEMENT') {
        showStatementPreview(preview);
      } else {
        showPreview(preview);
      }
    }

    function statementCategorySelectHtml(selectedId, idx, type) {
      const cats = importCategoriesFor(type);
      if (!cats.length) {
        return '<select data-row-category data-idx="' + idx + '" disabled>' +
          '<option value="">Sem categorias</option></select>';
      }
      const options = cats.map(function (c) {
        const sel = String(c.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.label) + '</option>';
      }).join('');
      return '<select data-row-category data-idx="' + idx + '" ' +
        'style="width:auto;font-size:12px;padding:4px 6px;">' + options + '</select>';
    }

    function stateBadge(state) {
      if (state === 'DUPLICATE') {
        return '<span class="badge badge-muted">já importado</span>';
      }
      if (state === 'RECONCILE') {
        return '<span class="badge badge-warning">concilia</span>';
      }
      return '<span class="badge badge-income">novo</span>';
    }

    function statementRowHtml(row, idx) {
      const state = row.state;
      const dup = state === 'DUPLICATE';
      const checked = row.checked !== undefined ? (row.checked ? 'checked' : '') : (dup ? '' : 'checked');
      const amt = Number(row.amount) || 0;
      const amtColor = amt < 0 ? 'var(--expense)' : 'var(--income)';
      const note = (state === 'RECONCILE' && row.reconcileDescription)
        ? ' <span style="color:var(--text-muted);font-size:11px;font-style:italic;">↔ ' + esc(row.reconcileDescription) + '</span>'
        : '';
      return (
        '<tr style="border-top:1px solid var(--border);' + (dup ? 'opacity:0.6;' : '') + '">' +
          '<td style="padding:8px 10px;text-align:center;">' +
            '<input type="checkbox" data-row-include data-idx="' + idx + '" ' + checked + ' ' +
              'style="width:16px;height:16px;cursor:pointer;" />' +
          '</td>' +
          '<td style="padding:8px 10px;width:90px;white-space:nowrap;color:var(--text-secondary);">' + esc(fmtIsoDate(row.date)) + '</td>' +
          '<td style="padding:8px 10px;">' + statementCategorySelectHtml(row.categoryId, idx, row.type) + '</td>' +
          '<td style="padding:8px 10px;">' +
            '<input type="text" data-row-description data-idx="' + idx + '" value="' + esc(row.description) + '" ' +
              'style="width:100%;font-size:12px;padding:4px 6px;border:1px solid transparent;background:transparent;color:inherit;outline:none;" ' +
              'onfocus="this.style.border=\'1px solid var(--border)\';this.style.background=\'var(--bg-card)\'" ' +
              'onblur="this.style.border=\'1px solid transparent\';this.style.background=\'transparent\'" />' +
            note +
          '</td>' +
          '<td style="padding:8px 10px;text-align:center;white-space:nowrap;">' + stateBadge(state) + '</td>' +
          '<td style="padding:8px 10px;text-align:right;font-weight:700;white-space:nowrap;color:' + amtColor + ';">' + esc(fmt(amt)) + '</td>' +
        '</tr>'
      );
    }

    function showStatementPreview(preview) {
      statementData = preview;
      const accounts = (preview && preview.candidateAccounts) || [];
      selectedAccountId = (preview && preview.selectedAccountId) || selectedAccountId ||
        (accounts[0] && accounts[0].id) || null;
      const rows = (preview && preview.rows) || [];

      const accOptions = (selectedAccountId ? '' : '<option value="" selected>Selecione a conta</option>') +
        accounts.map(function (a) {
          const sel = String(a.id) === String(selectedAccountId) ? ' selected' : '';
          return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
        }).join('');
      const accSelectorHtml = accounts.length
        ? '<div class="form-group" style="margin-top:12px;">' +
            '<label class="form-label" for="' + cardSelectId + '">Conta de destino</label>' +
            '<select id="' + cardSelectId + '" data-region="account-select">' + accOptions + '</select>' +
          '</div>'
        : '<p style="font-size:12px;color:var(--expense);margin-top:12px;">Nenhuma conta disponível para importação.</p>';

      function sortIcon(col) {
        if (sortCol !== col) return '';
        return sortAsc ? ' ↑' : ' ↓';
      }

      const tableRows = rows.map(statementRowHtml).join('');
      const tableHtml = rows.length
        ? '<div style="max-height:360px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius-sm);margin-top:12px;">' +
            '<table style="width:100%;border-collapse:collapse;font-size:13px;">' +
              '<thead><tr style="position:sticky;top:0;background:var(--bg-hover);">' +
                '<th style="padding:8px 10px;width:34px;text-align:center;">' +
                  '<input type="checkbox" data-act="select-all" checked style="width:16px;height:16px;cursor:pointer;" />' +
                '</th>' +
                '<th data-sort="date" style="padding:8px 10px;width:90px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Data' + sortIcon('date') + '</th>' +
                '<th data-sort="categoryId" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Categoria' + sortIcon('categoryId') + '</th>' +
                '<th data-sort="description" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Descrição' + sortIcon('description') + '</th>' +
                '<th data-sort="state" style="padding:8px 10px;text-align:center;font-size:11px;color:var(--text-muted);cursor:pointer;">Estado' + sortIcon('state') + '</th>' +
                '<th data-sort="amount" style="padding:8px 10px;text-align:right;font-size:11px;color:var(--text-muted);cursor:pointer;">Valor' + sortIcon('amount') + '</th>' +
              '</tr></thead>' +
              '<tbody>' + tableRows + '</tbody>' +
            '</table>' +
          '</div>'
        : '<p style="font-size:13px;color:var(--text-muted);margin-top:12px;">Nenhum lançamento encontrado no extrato.</p>';

      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + window.icon('check', 22) + '</span>' +
          '<div>' +
            '<p style="font-size:13px;color:var(--text-muted);">Extrato detectado</p>' +
            '<p style="font-size:18px;font-weight:800;">BTG Pactual</p>' +
          '</div>' +
        '</div>' +
        accSelectorHtml +
        tableHtml +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:10px;">' +
          esc(rows.length + ' lançamento(s) encontrado(s). Escolha a conta, revise os estados e confirme a importação.') +
        '</p>'
      );

      m.$el.find('[data-region=account-select]').on('change', function () {
        selectedAccountId = this.value || null;
        refreshStatementPreview();
      });

      m.$el.find('[data-act=do-import]').hide();
      const $footer = m.$el.find('.modal-footer');
      $footer.find('[data-act=do-statement-confirm]').remove();
      if (rows.length) {
        $footer.append(window.btn({
          variant: 'primary', size: 'md', icon: 'check', label: 'Confirmar importação',
          attrs: 'data-act="do-statement-confirm" type="button"'
        }));
      }
    }

    // Re-runs the preview against the chosen account so duplicate/reconcile states refresh.
    function refreshStatementPreview() {
      if (!selectedFile) return;
      window.App.TransactionService.importPreview(selectedFile, lastPassword, selectedAccountId)
        .then(function (preview) {
          if (preview && preview.documentType === 'BANK_STATEMENT') showStatementPreview(preview);
        })
        .catch(function (err) {
          window.toast((err && err.message) || 'Falha ao atualizar o preview', 'error');
        });
    }

    function confirmStatementImport() {
      if (!statementData) return;
      if (!selectedAccountId) { window.toast('Selecione a conta de destino', 'error'); return; }

      const rows = [];
      m.$el.find('[data-row-include]').each(function () {
        if (!this.checked) return;
        const idx = Number($(this).attr('data-idx'));
        const src = statementData.rows && statementData.rows[idx];
        if (!src) return;
        const $cat = m.$el.find('[data-row-category][data-idx="' + idx + '"]');
        const $desc = m.$el.find('[data-row-description][data-idx="' + idx + '"]');
        const categoryId = ($cat.val() || src.categoryId) || null;
        const description = ($desc.val() || src.description || '').trim();
        rows.push({
          description: description,
          amount: src.amount,
          date: src.date,
          type: src.type,
          categoryId: categoryId,
        });
      });

      if (!rows.length) { window.toast('Selecione ao menos um lançamento', 'error'); return; }

      const $btn = m.$el.find('[data-act=do-statement-confirm]').prop('disabled', true);
      window.App.TransactionService.importStatementConfirm({ accountId: selectedAccountId, rows: rows })
        .then(function (res) {
          showConfirmSummary((res && res.created) || 0, (res && res.skipped) || 0, (res && res.reconciled) || 0);
          return loadTransactions();
        })
        .catch(function (err) {
          $btn.prop('disabled', false);
          window.toast((err && err.message) || 'Falha ao confirmar a importação', 'error');
        });
    }

    m.$el.on('click', '[data-act=toggle-password]', function () { revealPassword(''); });

    m.$el.on('click', '[data-act=do-confirm]', function () { confirmImport(); });

    m.$el.on('click', '[data-act=do-statement-confirm]', function () { confirmStatementImport(); });

    m.$el.on('click', '[data-act=do-import]', function () {
      if (!selectedFile) { window.toast('Selecione um arquivo PDF', 'error'); return; }
      const password = ($pwd.val() || '').trim();
      lastPassword = password || null;
      const $btn = $(this).prop('disabled', true);

      window.App.TransactionService.importPreview(selectedFile, lastPassword, null)
        .then(function (preview) { routePreview(preview); })
        .catch(function (err) {
          $btn.prop('disabled', false);
          const code = err && err.code;
          if (code === 'PASSWORD_REQUIRED') {
            revealPassword('Este PDF está protegido. Informe a senha para continuar.');
            return;
          }
          if (code === 'WRONG_PASSWORD') {
            revealPassword('Senha incorreta. Verifique e tente novamente.');
            return;
          }
          window.toast((err && err.message) || 'Falha ao importar a fatura', 'error');
        });
    });

    m.$el.on('change', '[data-act=select-all]', function () {
      const checked = this.checked;
      m.$el.find('[data-row-include]').prop('checked', checked);
    });

    m.$el.on('click', '[data-sort]', function () {
      const col = $(this).attr('data-sort');
      const isStatement = !!statementData;
      const data = isStatement ? statementData : previewData;
      const renderFn = isStatement ? showStatementPreview : showPreview;

      // Save current states before sorting
      m.$el.find('[data-row-include]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].checked = this.checked;
      });
      m.$el.find('[data-row-category]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].categoryId = this.value;
      });
      m.$el.find('[data-row-description]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].description = this.value;
      });

      if (sortCol === col) {
        sortAsc = !sortAsc;
      } else {
        sortCol = col;
        sortAsc = true;
      }

      data.rows.sort(function (a, b) {
        let va = a[col];
        let vb = b[col];
        if (va == null) va = '';
        if (vb == null) vb = '';
        if (typeof va === 'string') va = va.toLowerCase();
        if (typeof vb === 'string') vb = vb.toLowerCase();

        if (va < vb) return sortAsc ? -1 : 1;
        if (va > vb) return sortAsc ? 1 : -1;
        return 0;
      });

      renderFn(data);
    });
  }

  // ── Modal: create / edit ──────────────────────────────────
  function openFormModal(existing) {
    const isEdit = !!existing;
    const isTransferEdit = isEdit && isTransferTx(existing);
    const uniq = Date.now();
    const ids = {
      desc: 'tx-desc-' + uniq,
      amount: 'tx-amount-' + uniq,
      date: 'tx-date-' + uniq,
      category: 'tx-cat-' + uniq,
      account: 'tx-acc-' + uniq,
      destAccount: 'tx-dest-' + uniq,
      costCenter: 'tx-cc-' + uniq,
      status: 'tx-status-' + uniq,
    };

    const initial = {
      type: isEdit ? (existing.type || 'expense') : 'expense',
      description: isEdit ? (existing.description || '') : '',
      amount: isEdit ? maskCurrency(String(Math.round(Math.abs(Number(existing.amount) || 0) * 100) / 100)) : '',
      date: isEdit ? (existing.date || '').slice(0, 10) : defaultNewDate(),
      categoryId: isEdit ? (existing.categoryId || '') : '',
      accountId: isEdit ? String(existing.accountId || '') : '',
      destAccountId: '',
      status: isEdit ? (existing.status === 'balance' ? 'confirmed' : (existing.status || 'confirmed')) : 'confirmed',
      costCenterId: isEdit ? String(existing.costCenterId || '') : '',
      isEstorno: isEdit ? (
        (existing.type === 'expense' && Number(existing.amount) > 0) ||
        (existing.type === 'income'  && Number(existing.amount) < 0)
      ) : false,
    };

    // Account options.
    const accs = accountsList().filter(function (a) { return a.active !== false || (isEdit && String(a.id) === initial.accountId); });
    if (!initial.accountId && accs.length) initial.accountId = String(accs[0].id);

    function natureForType(t) {
      if (t === 'income') return 'REVENUE';
      if (t === 'expense') return 'EXPENSE';
      return null;
    }

    function buildCatOptions(type, selectedId) {
      const nat = natureForType(type);
      const cats = flatCategories(nat, true);
      if (!cats.length) return '<option value="">Nenhuma categoria disponível</option>';
      return cats.map(function (c) {
        const sel = String(c.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.label) + '</option>';
      }).join('');
    }

    function buildAccountOpts(selectedId, includeEmpty) {
      if (!accs.length) return '<option value="">Nenhuma conta disponível</option>';
      const empty = includeEmpty ? '<option value="">— Selecione —</option>' : '';
      return empty + accs.map(function (a) {
        const sel = String(a.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
      }).join('');
    }

    function buildCostCenterOpts(selectedId) {
      const ccs = window.App.CacheStore.costCenters();
      if (!ccs.length) return '<option value="">Nenhum centro de custo</option>';
      const variavel = ccs.filter(function (c) { return /vari/i.test(c.description || c.name || ''); })[0];
      const target = selectedId || (variavel && variavel.id) || '';
      return ccs.map(function (c) {
        const label = c.description || c.name || '';
        const sel = String(c.id) === String(target) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(label) + '</option>';
      }).join('');
    }

    const accOpts = buildAccountOpts(initial.accountId, false);

    const statusOpts = [
      ['confirmed', 'Confirmado'],
      ['pending', 'Pendente'],
      ['scheduled', 'Agendado'],
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

    function buildGridHtml(type) {
      if (type === 'transfer') {
        return (
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.account + '">Origem</label>' +
            '<select id="' + ids.account + '" name="accountId">' + buildAccountOpts(initial.accountId, true) + '</select>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.destAccount + '">Destino</label>' +
            '<select id="' + ids.destAccount + '" name="destAccountId">' + buildAccountOpts(initial.destAccountId, true) + '</select>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.date + '">Data</label>' +
            '<input id="' + ids.date + '" name="date" type="date" required value="' + esc(initial.date) + '" />' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.amount + '">Valor (R$)</label>' +
            '<input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" ' +
              'placeholder="0,00" value="' + esc(initial.amount) + '" />' +
          '</div>'
        );
      }
      return (
        '<div class="form-group full">' +
          '<label class="form-label" for="' + ids.desc + '">Descrição</label>' +
          '<input id="' + ids.desc + '" name="description" type="text" required ' +
            'placeholder="Ex: Mercado, Salário..." value="' + esc(initial.description) + '" />' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.date + '">Data</label>' +
          '<input id="' + ids.date + '" name="date" type="date" required value="' + esc(initial.date) + '" />' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.account + '">Conta</label>' +
          '<select id="' + ids.account + '" name="accountId">' + accOpts + '</select>' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.costCenter + '">Centro de Custo</label>' +
          '<select id="' + ids.costCenter + '" name="costCenterId">' + buildCostCenterOpts(initial.costCenterId) + '</select>' +
        '</div>' +
        '<div class="form-group">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">' +
            '<label class="form-label" for="' + ids.category + '" style="margin:0;">Categoria</label>' +
            '<button type="button" data-act="new-category" ' +
              'style="background:none;border:none;color:var(--accent);cursor:pointer;' +
              'font-size:11px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' +
              window.icon('plus', 12) + 'Nova categoria' +
            '</button>' +
          '</div>' +
          '<select id="' + ids.category + '" name="categoryId" data-region="category-select">' +
            buildCatOptions(type, initial.categoryId) +
          '</select>' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.amount + '">Valor (R$)</label>' +
          '<input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" ' +
            'placeholder="0,00" value="' + esc(initial.amount) + '" />' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.status + '">Status</label>' +
          '<select id="' + ids.status + '" name="status">' + statusOpts + '</select>' +
        '</div>' +
        '<div class="form-group full">' +
          '<label style="display:inline-flex;align-items:center;gap:8px;cursor:pointer;' +
            'font-size:13px;font-weight:500;color:var(--text-secondary);">' +
            '<input type="checkbox" name="estorno"' + (initial.isEstorno ? ' checked' : '') +
              ' style="width:16px;height:16px;accent-color:var(--accent);" />' +
            'Estorno (inverter sinal)' +
          '</label>' +
        '</div>'
      );
    }

    const bodyHtml =
      '<form data-form="tx" autocomplete="off">' +
        (isTransferEdit
          ? '<div style="display:flex;gap:8px;align-items:flex-start;padding:10px 12px;margin-bottom:14px;' +
              'border:1px solid var(--warning);border-radius:var(--radius-sm);background:var(--warning-light, transparent);">' +
              '<span style="color:var(--warning);flex-shrink:0;display:flex;">' + window.icon('alertCircle', 16) + '</span>' +
              '<span style="font-size:12px;color:var(--text-secondary);line-height:1.4;">' +
                'Editar uma transferência desfaz o par: a perna oposta será removida e este lançamento passa a ser avulso.' +
              '</span>' +
            '</div>'
          : '') +
        '<div data-region="type-row" style="display:flex;gap:8px;margin-bottom:16px;">' +
          typeBtnHtml('expense',  '↓ Despesa',       'expense',  initial.type === 'expense') +
          typeBtnHtml('income',   '↑ Receita',       'income',   initial.type === 'income') +
          typeBtnHtml('transfer', '⇄ Transferência', 'accent',   initial.type === 'transfer') +
        '</div>' +
        '<input type="hidden" name="type" value="' + esc(initial.type) + '" />' +
        '<div class="form-grid" data-region="grid">' +
          buildGridHtml(initial.type) +
        '</div>' +
      '</form>';

    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $save = window.btn({
      variant: 'primary', size: 'md', label: 'Salvar',
      attrs: 'data-act="save" type="submit"'
    });
    const $footer = $('<div style="display:flex;gap:10px;"></div>').append($cancel).append($save);

    const m = window.modal({
      title: isEdit ? 'Editar Lançamento' : 'Novo Lançamento',
      body: bodyHtml,
      footer: $footer[0].outerHTML,
    });
    m.open();

    const $form = m.$body.find('form[data-form=tx]');
    function bindAmountMask() {
      if (window.bindCurrencyMask) window.bindCurrencyMask($form.find('input[name=amount]'));
    }
    bindAmountMask();

    // Type buttons sync hidden input + rebuild grid layout for the chosen type.
    m.$body.on('click', '[data-act=set-form-type]', function (e) {
      e.preventDefault();
      const t = $(this).attr('data-type');
      // Snapshot current values to preserve across rebuild.
      initial.description = $form.find('input[name=description]').val() || initial.description;
      initial.amount      = $form.find('input[name=amount]').val()      || initial.amount;
      initial.date        = $form.find('input[name=date]').val()        || initial.date;
      initial.accountId   = $form.find('select[name=accountId]').val()  || initial.accountId;
      const $destSel      = $form.find('select[name=destAccountId]');
      if ($destSel.length) initial.destAccountId = $destSel.val() || initial.destAccountId;
      const $catSel       = $form.find('select[name=categoryId]');
      if ($catSel.length)  initial.categoryId    = $catSel.val()       || initial.categoryId;
      const $stSel        = $form.find('select[name=status]');
      if ($stSel.length)   initial.status        = $stSel.val()        || initial.status;
      const $ccSel        = $form.find('select[name=costCenterId]');
      if ($ccSel.length)   initial.costCenterId  = $ccSel.val()        || initial.costCenterId;
      const $estornoChk   = $form.find('input[name=estorno]');
      if ($estornoChk.length) initial.isEstorno  = $estornoChk.is(':checked');

      $form.find('input[name=type]').val(t);
      // Repaint type row.
      const $row = m.$body.find('[data-region=type-row]');
      $row.empty();
      $row.append(typeBtnHtml('expense',  '↓ Despesa',       'expense',  t === 'expense'));
      $row.append(typeBtnHtml('income',   '↑ Receita',       'income',   t === 'income'));
      $row.append(typeBtnHtml('transfer', '⇄ Transferência', 'accent',   t === 'transfer'));
      // Rebuild grid.
      m.$body.find('[data-region=grid]').html(buildGridHtml(t));
      bindAmountMask();
    });

    // "+ Nova categoria": quick-create a category inline, then select it.
    m.$body.on('click', '[data-act=new-category]', function (e) {
      e.preventDefault();
      const nature = natureForType($form.find('input[name=type]').val());
      if (!nature) return; // transfer has no category
      openCategoryCreateModal(nature, function (created) {
        const $sel = $form.find('select[name=categoryId]');
        // Drop the "Nenhuma categoria disponível" placeholder, if present.
        $sel.find('option').each(function () { if (!this.value) $(this).remove(); });
        if (!$sel.find('option[value="' + esc(created.id) + '"]').length) {
          $sel.append('<option value="' + esc(created.id) + '">' +
            esc(quickCategoryLabel(created)) + '</option>');
        }
        $sel.val(String(created.id));
        initial.categoryId = String(created.id);
      });
    });

    function submit(e) {
      if (e) e.preventDefault();
      const type = $form.find('input[name=type]').val();
      const amtRaw = $form.find('input[name=amount]').val();
      const amt = window.parseCurrency(amtRaw);
      const date = $form.find('input[name=date]').val();
      const accountId = $form.find('select[name=accountId]').val();
      const $btn = m.$el.find('[data-act=save]');

      if (type === 'transfer') {
        const destAccountId = $form.find('select[name=destAccountId]').val();
        if (!accountId) { window.toast('Selecione a conta de origem', 'error'); return; }
        if (!destAccountId) { window.toast('Selecione a conta de destino', 'error'); return; }
        if (!window.Domain.Transaction.isValidTransfer(accountId, destAccountId)) {
          window.toast('Origem e destino devem ser contas diferentes', 'error'); return;
        }
        if (!date) { window.toast('Informe a data', 'error'); return; }
        if (!isFinite(amt) || amt <= 0) { $form.find('input[name=amount]').trigger('focus'); return; }

        $btn.prop('disabled', true);
        window.App.TransactionService.transfer({
          fromAccountId: accountId,
          toAccountId: destAccountId,
          date: date,
          amount: Number(amt.toFixed(2)),
        }).then(function () {
          // Converting an existing lançamento into a transfer: drop the original
          // so its amount isn't counted twice alongside the new transfer pair.
          if (isEdit) return window.App.TransactionService.remove(existing.accountId, existing.id);
        }).then(function () {
          m.close();
          window.toast(isEdit ? 'Lançamento convertido em transferência' : 'Transferência registrada', 'success');
          return loadTransactions();
        }).catch(function (err) {
          $btn.prop('disabled', false);
          window.toast((err && err.message) || 'Falha ao registrar transferência', 'error');
        });
        return;
      }

      const description = ($form.find('input[name=description]').val() || '').trim();
      if (!description) { $form.find('input[name=description]').trigger('focus'); return; }
      if (!isFinite(amt) || amt <= 0) { $form.find('input[name=amount]').trigger('focus'); return; }
      const categoryId = $form.find('select[name=categoryId]').val();
      const costCenterId = $form.find('select[name=costCenterId]').val();
      const status = $form.find('select[name=status]').val();
      const isEstorno = $form.find('input[name=estorno]').is(':checked');

      if (!accountId) { window.toast('Selecione uma conta', 'error'); return; }
      if (!categoryId) { window.toast('Selecione uma categoria', 'error'); return; }
      if (!costCenterId) { window.toast('Selecione o centro de custo', 'error'); return; }

      const signed = window.Domain.Transaction.signedAmount(type, amt) * (isEstorno ? -1 : 1);
      const payload = {
        description: description,
        amount: Number(signed.toFixed(2)),
        date: date,
        categoryId: categoryId || null,
        accountId: accountId,
        costCenterId: costCenterId,
        status: status,
        type: type,
      };

      $btn.prop('disabled', true);
      const p = isEdit
        ? window.App.TransactionService.update(existing.id, payload)
        : window.App.TransactionService.create(payload);

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Lançamento atualizado' : 'Lançamento criado', 'success');
        return loadTransactions();
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao salvar lançamento', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=save]', submit);
  }

  // ── Modal: quick-create category (nested, from the tx form) ─
  // Label for a freshly-created category not yet in the cache (SSE lag).
  function quickCategoryLabel(cat) {
    const name = cat.name || cat.description || '';
    if (cat.parentId) {
      const parent = window.categoryById()[cat.parentId];
      if (parent) return parent.name + ' / ' + name;
    }
    return name;
  }

  // Nature is fixed by the originating transaction type. On success the created
  // category (with id) is handed to `onCreated` so the caller can select it.
  function openCategoryCreateModal(nature, onCreated) {
    const uniq = Date.now();
    const nameId = 'qcat-name-' + uniq;
    const parentSelId = 'qcat-parent-' + uniq;
    const natureLabel = nature === 'REVENUE' ? 'Receita' : 'Despesa';

    const roots = window.App.CacheStore.categories().filter(function (c) {
      return String(c.nature || '').toUpperCase() === nature && !c.parentId;
    }).slice().sort(window.sortByName);

    const parentOpts = '<option value="">— Nenhuma (categoria raiz) —</option>' +
      roots.map(function (p) {
        return '<option value="' + esc(p.id) + '">' + esc(p.name) + '</option>';
      }).join('');

    const bodyHtml =
      '<form data-form="qcat" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label">Tipo</label>' +
            '<input type="text" value="' + esc(natureLabel) + '" disabled />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + nameId + '">Nome</label>' +
            '<input id="' + nameId + '" name="name" type="text" required ' +
              'placeholder="Nome da categoria" />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + parentSelId + '">Categoria Pai (opcional)</label>' +
            '<select id="' + parentSelId + '" name="parentId">' + parentOpts + '</select>' +
          '</div>' +
        '</div>' +
      '</form>';

    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $save = window.btn({
      variant: 'primary', size: 'md', label: 'Salvar',
      attrs: 'data-act="qcat-save" type="submit"'
    });
    const $footer = $('<div style="display:flex;gap:10px;"></div>').append($cancel).append($save);

    const m = window.modal({
      title: 'Nova Categoria',
      body: bodyHtml,
      footer: $footer[0].outerHTML,
    });
    m.open();

    const $form = m.$body.find('form[data-form=qcat]');
    $form.find('input[name=name]').trigger('focus');

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($form.find('input[name=name]').val() || '').trim();
      if (!name) { $form.find('input[name=name]').trigger('focus'); return; }
      const parentId = $form.find('select[name=parentId]').val() || null;

      const $btn = m.$el.find('[data-act=qcat-save]').prop('disabled', true);
      window.App.CategoryService.create({
        name: name,
        nature: nature,
        parentId: parentId,
      }).then(function (created) {
        m.close();
        window.toast('Categoria criada', 'success');
        if (onCreated) onCreated(created);
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao criar categoria', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=qcat-save]', submit);
  }

  // ── Delete (with scope for grouped/recurring) ─────────────
  function openDeleteModal(tx) {
    const isTransfer = isTransferTx(tx);
    // Transfer legs carry a groupId but are not installments — never offer the parcelas scope for them.
    const isGrouped = !!tx.groupId && !isTransfer;

    if (!isGrouped) {
      // Simple confirm. A transfer always removes both legs server-side.
      let bodyHtml =
        '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
          (isTransfer
            ? 'Excluir esta transferência? As duas pernas (saída e entrada) serão removidas. Esta ação não pode ser desfeita.'
            : 'Excluir <strong>' + esc(tx.description || 'lançamento') + '</strong>? ' +
              'Esta ação não pode ser desfeita.') +
        '</p>';
      const $cancel = window.btn({
        variant: 'secondary', size: 'md', label: 'Cancelar',
        attrs: 'data-modal-close="1" type="button"'
      });
      const $confirm = window.btn({
        variant: 'danger', size: 'md', icon: 'trash', label: 'Excluir',
        attrs: 'data-act="confirm-delete" type="button"'
      });
      const $footer = $('<div style="display:flex;gap:10px;"></div>').append($cancel).append($confirm);

      const m = window.modal({ title: isTransfer ? 'Excluir Transferência' : 'Excluir Lançamento', body: bodyHtml, footer: $footer[0].outerHTML });
      m.open();
      m.$el.on('click', '[data-act=confirm-delete]', function () {
        const $b = $(this).prop('disabled', true);
        window.App.TransactionService.remove(tx.accountId, tx.id).then(function () {
          m.close();
          window.toast('Lançamento excluído', 'success');
          return loadTransactions();
        }).catch(function (err) {
          $b.prop('disabled', false);
          window.toast((err && err.message) || 'Falha ao excluir', 'error');
        });
      });
      return;
    }

    // Grouped: scope choice.
    let bodyHtml =
      '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
        'Este lançamento faz parte de um grupo de parcelas. ' +
        'Como deseja excluí-lo?' +
      '</p>';
    const $only = window.btn({
      variant: 'secondary', size: 'md', label: 'Apenas este',
      attrs: 'data-act="del-single" type="button"'
    });
    const $future = window.btn({
      variant: 'secondary', size: 'md', label: 'Este e seguintes',
      attrs: 'data-act="del-future" type="button"'
    });
    const $all = window.btn({
      variant: 'danger', size: 'md', icon: 'trash', label: 'Todos',
      attrs: 'data-act="del-all" type="button"'
    });
    const $cancel2 = window.btn({
      variant: 'ghost', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $footer2 = $('<div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end;"></div>')
      .append($cancel2).append($only).append($future).append($all);

    const m2 = window.modal({
      title: 'Excluir Lançamento Recorrente',
      body: bodyHtml,
      footer: $footer2[0].outerHTML,
    });
    m2.open();

    function doRemove(mode) {
      const $btns = m2.$el.find('button').prop('disabled', true);
      window.App.TransactionService.remove(tx.accountId, tx.id, mode).then(function () {
        m2.close();
        window.toast('Lançamento(s) excluído(s)', 'success');
        return loadTransactions();
      }).catch(function (err) {
        $btns.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao excluir', 'error');
      });
    }
    m2.$el.on('click', '[data-act=del-single]', function () { doRemove('SINGLE'); });
    m2.$el.on('click', '[data-act=del-future]', function () { doRemove('FUTURE'); });
    m2.$el.on('click', '[data-act=del-all]',    function () { doRemove('ALL'); });
  }

  // ── Quick mark as paid ────────────────────────────────────
  function markPaid(tx) {
    const today = new Date().toISOString().slice(0, 10);
    window.App.TransactionService.patchStatus(tx.accountId, tx.id, 'confirmed', today).then(function () {
      window.toast('Lançamento confirmado', 'success');
      return loadTransactions();
    }).catch(function (err) {
      window.toast((err && err.message) || 'Falha ao confirmar', 'error');
    });
  }

  // ── Event delegation ──────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.tx', '[data-act=new]', function () { openFormModal(null); });
    $root.on('click.tx', '[data-act=import]', function () { openImportModal(); });

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
      if (tx) openDeleteModal(tx);
    });
    $root.on('click.tx', '[data-act=mark-paid]', function (e) {
      e.stopPropagation();
      const tx = findTx($(this).attr('data-id'));
      if (tx) markPaid(tx);
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

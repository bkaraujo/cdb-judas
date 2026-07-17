/* pages/statement.js — Extrato de Contas.
 * Layout 280px / 1fr: lista de contas (esquerda) + Card de lançamentos com saldo corrente (direita).
 * Conta selecionável; período navegável (mês/ano). App.StatementService monta os itens no cliente
 * (transações do período + snapshot de saldo mensal): StatementItem[] = { date, description, amount,
 * status, runningBal, categoryId? }. A coluna da esquerda usa App.StatementService.summary.
 * Coluna fixa categoria/subcategoria (largura = maior label possível) entre data e descrição.
 * Status 'balance' => linha "Saldo anterior" (sem amount, runningBal = saldo de abertura).
 */
(function () {
  window.Pages = window.Pages || {};

  let state = null;

  function resetState() {
    const now = new Date();
    state = {
      $root: null,
      accountId: null,
      month: now.getMonth() + 1, // 1-12 (backend usa 1-based)
      year: now.getFullYear(),
      items: [],
      summary: {}, // panorama por conta no período (accountId -> resumo)
      loading: false,
    };
  }

  // ── Helpers ───────────────────────────────────────────────


  function checkingAccounts() {
    return window.App.CacheStore.accounts()
      .slice()
      .sort(window.sortByName);
  }

  function selectedAccount() {
    return window.byId(checkingAccounts(), state.accountId);
  }

  // ── Fetch ─────────────────────────────────────────────────
  function loadStatement() {
    if (!state.accountId) {
      state.items = [];
      state.loading = false;
      render();
      return Promise.resolve();
    }
    state.loading = true;
    render();
    const account = selectedAccount();
    const sum = state.summary[String(state.accountId)];
    const openingBalance = sum ? sum.openingBalance : (account ? (+account.balance || 0) : 0);
    return window.App.StatementService.load(
        state.accountId,
        window.Domain.Period.create(state.month, state.year),
        openingBalance
      )
      .then(function (list) {
        state.items = Array.isArray(list) ? list : [];
        state.loading = false;
        render();
      })
      .catch(function (err) {
        state.items = [];
        state.loading = false;
        render();
        window.toast((err && err.message) || 'Falha ao carregar extrato', 'error');
      });
  }

  // Panorama do mês por conta (saldo inicial/final + totais) para a coluna da esquerda.
  function loadSummary() {
    return window.App.StatementService.summary(window.Domain.Period.create(state.month, state.year))
      .then(function (list) {
        const map = {};
        (Array.isArray(list) ? list : []).forEach(function (s) { map[String(s.accountId)] = s; });
        state.summary = map;
        render();
      })
      .catch(function () { /* panorama é complementar; silencioso */ });
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $page = $('<div class="fade-in"></div>');

    // Header
    const $header = $(
      '<div class="page-header">' +
        '<h1>Extrato de Contas</h1>' +
        '<div class="page-header-actions" data-region="head-actions"></div>' +
      '</div>'
    );
    const $periodNav = window.periodNav({
      label: window.monthLabel(state.month, state.year),
      onPrev: function () { window.shiftMonth(state, -1, true); loadSummary().then(loadStatement); },
      onNext: function () { window.shiftMonth(state, +1, true); loadSummary().then(loadStatement); },
    });
    $header.find('[data-region=head-actions]').append($periodNav);
    $page.append($header);

    // Grid 280px / 1fr
    const $grid = $('<div style="display:grid;grid-template-columns:280px 1fr;gap:16px;"></div>');

    // Left column: accounts list
    const $left = $('<div style="display:flex;flex-direction:column;gap:12px;"></div>');
    const accs = checkingAccounts();
    if (accs.length === 0) {
      $left.append(
        '<div style="font-size:13px;color:var(--text-muted);padding:14px 16px;' +
        'background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius);">' +
          'Nenhuma conta disponível.' +
        '</div>'
      );
    } else {
      accs.forEach(function (a) {
        const active = String(a.id) === String(state.accountId);
        const sum = state.summary[String(a.id)];
        const bal = sum ? (Number(sum.closingBalance) || 0) : window.Domain.Account.currentBalance(a);
        const btnStyle =
          'padding:14px 16px;border-radius:var(--radius);text-align:left;' +
          'background:' + (active ? 'var(--accent-light)' : 'var(--bg-card)') + ';' +
          'border:1px solid ' + (active ? 'var(--accent)' : 'var(--border)') + ';' +
          'color:' + (active ? 'var(--accent)' : 'var(--text-primary)') + ';' +
          'cursor:pointer;font-weight:' + (active ? '700' : '400') + ';font-size:13px;' +
          'transition:all var(--transition);display:flex;flex-direction:column;gap:4px;';
        const balColor = window.valueColor(bal);
        $left.append(
          '<button type="button" class="stm" data-act="select-account" ' +
            'data-id="' + esc(a.id) + '" style="' + btnStyle + '">' +
            '<span>' + esc(a.name) + '</span>' +
            '<span style="font-size:11px;color:' + balColor + ';font-weight:' +
              (active ? '700' : '500') + ';">' + esc(fmt(bal)) + '</span>' +
          '</button>'
        );
      });
    }
    $grid.append($left);

    // Right column: statement card
    const $card = $('<div class="card" style="padding:0;overflow:hidden;"></div>');

    if (!state.accountId) {
      $card.append(window.emptyState({
        icon: 'bookOpen',
        title: 'Selecione uma conta',
        desc: 'Escolha uma conta à esquerda para visualizar o extrato.'
      }));
    } else if (state.loading) {
      $card.append(window.emptyState({ icon: 'bookOpen', title: 'Carregando…' }));
    } else if (state.items.length === 0) {
      $card.append(window.emptyState({
        icon: 'bookOpen',
        title: 'Nenhum lançamento neste período',
        desc: 'Tente outro mês ou selecione outra conta.'
      }));
    } else {
      const items = state.items;
      // Fixed category column: width of the widest possible category label so
      // every description starts at the same x, regardless of each row's category.
      const catMap = window.categoryById();
      const catLens = window.flatCategories().map(function (c) { return c.label.length; });
      const catColCh = (catLens.length ? Math.max.apply(null, catLens) : 12) + 1;
      items.forEach(function (tx, i) {
        const isLast = i === items.length - 1;
        const isBalance = window.Domain.StatementItem.isBalanceHeader(tx);
        const cat = isBalance ? null : catMap[tx.categoryId];
        const catLbl = cat ? window.categoryLabel(cat) : '';
        const amt = Number(tx.amount) || 0;
        const amtColor = window.valueColor(amt);
        const dotColor =
          tx.status === 'confirmed' ? 'var(--income)' :
          isBalance ? 'var(--text-muted)' : 'var(--warning)';
        const runningBal = window.Domain.StatementItem.runningBalance(tx);

        const rowStyle =
          'display:flex;align-items:center;gap:16px;padding:11px 20px;' +
          (isLast ? '' : 'border-bottom:1px solid var(--border-light);') +
          'background:' + (isBalance ? 'var(--bg-hover)' : 'transparent') + ';' +
          'transition:background var(--transition);';

        const descStyle =
          'flex:1;font-size:13px;font-weight:' + (isBalance ? '700' : '500') + ';' +
          'color:' + (isBalance ? 'var(--text-secondary)' : 'var(--text-primary)') + ';' +
          'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-transform:uppercase;';

        const catStyle =
          'flex:0 0 ' + catColCh + 'ch;width:' + catColCh + 'ch;' +
          'font-size:12px;color:var(--text-muted);' +
          'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

        const amountHtml = (!isBalance && amt !== 0)
          ? '<span style="font-size:13px;font-weight:700;color:' + amtColor + ';">' +
              esc(fmt(amt)) +
            '</span>'
          : '';

        $card.append(
          '<div style="' + rowStyle + '">' +
            '<span style="font-size:12px;color:var(--text-muted);min-width:56px;">' +
              esc(fmtDate(tx.date)) +
            '</span>' +
            '<span style="' + catStyle + '">' + esc(catLbl) + '</span>' +
            '<span style="' + descStyle + '">' + esc(tx.description || '—') + '</span>' +
            amountHtml +
            '<span style="font-size:13px;font-weight:700;color:' + window.valueColor(runningBal) + ';min-width:100px;text-align:right;">' +
              esc(fmt(runningBal)) +
            '</span>' +
            '<div style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + dotColor + ';"></div>' +
          '</div>'
        );
      });
    }

    $grid.append($card);
    $page.append($grid);

    $root.empty().append($page);
  }

  // ── Event delegation ──────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.stm', '[data-act=select-account]', function () {
      const id = $(this).attr('data-id');
      if (id && String(id) !== String(state.accountId)) {
        state.accountId = id;
        loadStatement();
      }
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['statement'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);

      // Default-select first checking account if available.
      const accs = checkingAccounts();
      if (accs.length > 0) {
        state.accountId = String(accs[0].id);
      }
      render();
      loadSummary().then(function () {
        if (state.accountId) loadStatement();
      });
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.stm');
      }
      state = null;
    }
  };
})();

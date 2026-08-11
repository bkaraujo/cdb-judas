/* pages/statement.js — Extrato de Contas.
 * Layout 280px / 1fr: lista de contas (esquerda) + Card de lançamentos com saldo corrente (direita).
 * Conta selecionável; período navegável (mês/ano). App.StatementService monta os itens no cliente
 * (transações do período + snapshot de saldo mensal): StatementItem[] = { date, description, amount,
 * status, runningBal, categoryId? }. A coluna da esquerda usa App.StatementService.summary.
 * Coluna fixa categoria/subcategoria (largura = maior label possível) entre data e descrição.
 * Status 'balance' => linha "Saldo anterior" (sem amount, runningBal = saldo de abertura).
 * Compra de cartão não aparece linha a linha: App.StatementService a colapsa numa linha por
 * (cartão, vencimento) — 'FATURA · CONTA 1234', lançada na data de vencimento (mesmo mês em que o
 * backend a debita) e clicável para #/card-statement/{cardId}. Linha de fatura é derivada: sem
 * ações de editar/excluir, como a linha de saldo.
 */
(function () {
  window.Pages = window.Pages || {};

  let state = null;

  function resetState() {
    const p = window.App.PeriodService.get(); // { month: 1-12, year }
    state = {
      $root: null,
      accountId: null,
      month: p.month, // 1-12 (backend usa 1-based)
      year: p.year,
      items: [],
      summary: {}, // panorama por conta no período (accountId -> resumo)
      txIndex: [], // lançamentos do mês (todas as contas) — para editar/excluir e detectar transferências
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

  function currentPeriod() {
    return window.Domain.Period.create(state.month, state.year);
  }

  // Full transaction (all fields) behind a statement row, looked up in the month index.
  function findFullTx(id) {
    return window.byId(state.txIndex, id);
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
        currentPeriod(),
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
    return window.App.StatementService.summary(currentPeriod())
      .then(function (list) {
        const map = {};
        (Array.isArray(list) ? list : []).forEach(function (s) { map[String(s.accountId)] = s; });
        state.summary = map;
        render();
      })
      .catch(function () { /* panorama é complementar; silencioso */ });
  }

  // Índice do mês (todas as contas): usado para resolver o lançamento completo por trás de
  // uma linha do extrato (editar/excluir) e para detectar transferências — o par de uma
  // transferência vive em outra conta, invisível na visão de conta única, mas presente aqui.
  function loadMonthIndex() {
    const b = window.Domain.Period.bounds(currentPeriod());
    return window.App.TransactionService.list('dateFrom=' + b.from + '&dateTo=' + b.to)
      .then(function (list) { state.txIndex = Array.isArray(list) ? list : []; })
      .catch(function () { state.txIndex = []; });
  }

  // Recargas escopadas ao período (montagem + navegação de mês): panorama + índice; o extrato
  // detalhado (escopado à conta selecionada) roda em seguida.
  function reloadPeriod() {
    return Promise.all([loadSummary(), loadMonthIndex()]).then(loadStatement);
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $page = $('<div class="fade-in"></div>');

    // Header — sticky at top so it stays visible while the card content scrolls.
    const $header = $(
      '<div class="page-header">' +
        '<h1>Extrato de Contas</h1>' +
        '<div class="page-header-actions" data-region="head-actions"></div>' +
      '</div>'
    );
    const $periodNav = window.periodNav({
      month: state.month,
      year: state.year,
      onPrev: function () { window.shiftMonth(state, -1, true); reloadPeriod(); },
      onNext: function () { window.shiftMonth(state, +1, true); reloadPeriod(); },
      onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m; state.year = y; reloadPeriod(); },
    });
    $header.find('[data-region=head-actions]').append($periodNav);

    const $sticky = $(
      '<div style="position:sticky;top:0;z-index:5;background:var(--bg-base);"></div>'
    );
    $sticky.append($header);
    $page.append($sticky);

    // Grid 280px / 1fr — bounded height so the right card scrolls its own content
    // instead of the whole page.
    const $grid = $(
      '<div style="display:grid;grid-template-columns:280px 1fr;gap:16px;' +
        'height:calc(100vh - 160px);min-height:280px;"></div>'
    );

    // Left column: accounts list (own scrollbar so it doesn't stretch the grid row)
    const $left = $('<div style="display:flex;flex-direction:column;gap:12px;overflow-y:auto;"></div>');
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

    // Right column: statement card — scrolls its own content, header/left column stay put.
    const $card = $('<div class="card" style="padding:0;overflow-y:auto;"></div>');

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

        // Row actions (edit/delete) resolve the full transaction from the month index by id;
        // as linhas "Saldo anterior" e de fatura não têm nenhuma (célula vazia mantém as colunas
        // alinhadas) — a fatura é derivada, edita-se no extrato do cartão.
        const actionsHtml = (isBalance || tx.invoice) ? '' :
          '<button type="button" class="icon-btn" title="Editar" ' +
            'data-act="edit" data-id="' + esc(tx.id) + '" style="width:28px;height:28px;">' +
            window.icon('edit', 14) +
          '</button>' +
          '<button type="button" class="icon-btn" title="Excluir" ' +
            'data-act="trash" data-id="' + esc(tx.id) + '" style="width:28px;height:28px;color:var(--expense);">' +
            window.icon('trash', 14) +
          '</button>';

        // O link é um <a href> de verdade: o router é hash-based, então a navegação não precisa
        // de handler (e o deep-link fica copiável/abrível em nova aba).
        const descText = window.Domain.Transaction.describe(tx) || '—';
        const descHtml = tx.invoice
          ? '<a href="#/card-statement/' + esc(tx.cardId) + '" style="' + descStyle +
              'color:var(--accent);text-decoration:none;">' +
              esc(descText) +
            '</a>'
          : '<span style="' + descStyle + '">' + esc(descText) + '</span>';

        $card.append(
          '<div class="stm-row" data-id="' + esc(tx.id || '') + '" style="' + rowStyle + '">' +
            '<span style="font-size:12px;color:var(--text-muted);min-width:56px;">' +
              esc(fmtDate(tx.date)) +
            '</span>' +
            '<span style="' + catStyle + '">' + esc(catLbl) + '</span>' +
            window.tagFlagHtml(tx.tagIds) +
            descHtml +
            amountHtml +
            '<span style="font-size:13px;font-weight:700;color:' + window.valueColor(runningBal) + ';min-width:100px;text-align:right;">' +
              esc(fmt(runningBal)) +
            '</span>' +
            '<div style="width:8px;height:8px;border-radius:50%;flex-shrink:0;background:' + dotColor + ';"></div>' +
            '<div class="stm-row-actions">' + actionsHtml + '</div>' +
          '</div>'
        );
      });
    }

    $grid.append($card);
    $page.append($grid);

    // Footer: how many transactions are on display (excludes the "Saldo anterior" header row).
    if (state.accountId && !state.loading) {
      const txCount = state.items.filter(function (it) {
        return !window.Domain.StatementItem.isBalanceHeader(it);
      }).length;
      $page.append(
        '<div style="text-align:right;padding:12px 4px 0;font-size:12px;color:var(--text-muted);">' +
          esc(txCount + (txCount === 1 ? ' transação exibida' : ' transações exibidas')) +
        '</div>'
      );
    }

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

    // Row actions delegate to the shared transaction actions; the full transaction (all
    // fields the modals need) comes from the month index, keyed by the row's tx id.
    $root.on('click.stm', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const tx = findFullTx($(this).attr('data-id'));
      if (!tx) { window.toast('Lançamento indisponível — recarregue o período', 'error'); return; }
      window.transactionActions.openFormModal({
        existing: tx,
        list: state.txIndex,
        defaultDate: window.Domain.Period.bounds(currentPeriod()).from,
        onSaved: reloadPeriod,
      });
    });
    $root.on('click.stm', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const tx = findFullTx($(this).attr('data-id'));
      if (!tx) { window.toast('Lançamento indisponível — recarregue o período', 'error'); return; }
      window.transactionActions.openDeleteModal(tx, { list: state.txIndex, onDone: reloadPeriod });
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
      reloadPeriod();
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.stm');
      }
      state = null;
    }
  };
})();

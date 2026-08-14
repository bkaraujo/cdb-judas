/* feature/statement.js — fatia Extrato de Contas, vista computada sobre accounts+transactions
 * (sem endpoint backend próprio). Um arquivo por fatia: application → infrastructure/primary,
 * cada bloco abaixo é um IIFE independente (comentário original de cada arquivo preservado como
 * separador de seção). Sem domain própria e sem secondary: lê via TransactionsApi/AccountsApi
 * (fatias irmãs), nunca repositório cru — fecha V1/V3/V4. */

/* application — Extrato orchestration.
 * Assembles statement rows client-side from the existing transactions + monthly-balance
 * endpoints (no /statements backend). Stable interface: load(accountId, period) / summary(period). */
(function () {
  let txRepo = null;
  let balance = null;
  let cache = null;

  function init(deps) {
    txRepo = deps.txRepo;
    balance = deps.balance;
    cache = deps.cache;
    return { ready: true };
  }

  function checkings() {
    return cache.accounts();
  }

  /* Detail for one account in a period: a "Saldo anterior" row carrying the opening
     balance (previous period's closing, resolved by summary() — no balance fetch here),
     then each transaction (all statuses) with its running balance.
     Compras de cartão não entram linha a linha nem cartão a cartão: Domain.Invoice.collapse as
     troca por uma linha por (cartão, vencimento) e Domain.Invoice.mergeCards funde os cartões da
     conta numa linha 'Cartões de crédito' por vencimento — a mesma linha da tela de Lançamentos,
     lançada na data de vencimento, o mesmo mês em que o backend as debita no F002_BALANCE. Como o
     ciclo da fatura começa no mês anterior, a busca usa a janela alargada de
     Domain.Invoice.fetchWindow, não os bounds do período. */
  function load(accountId, period, openingBalance) {
    const account = cache.findById('accounts', accountId);
    if (!account) return Promise.resolve([]);
    const b = window.Domain.Period.bounds(period);
    const w = window.Domain.Invoice.fetchWindow([account], period);
    return txRepo.listByAccount(accountId, 'dateFrom=' + w.from + '&dateTo=' + w.to)
      .then(function (txs) {
        const rows = window.Domain.Invoice.collapse(Array.isArray(txs) ? txs : [], [account], period);
        return window.Domain.StatementItem.buildRows(openingBalance, window.Domain.Invoice.mergeCards(rows), b.from);
      });
  }

  /* Left-column panorama: closing + opening (previous period's closing) balance per
     checking account for the period. Two batch requests total (GET /accounts/balance for
     the period and for period-1) instead of one per account — fetched once per period
     (mount + prev/next), never per account selection; the statement screen reuses this
     cache instead of re-fetching. Accounts absent from a batch response (no snapshot yet)
     fall back to the account's current/initial balance. */
  function summary(period) {
    const prevPeriod = window.Domain.Period.shift(period, -1);
    function byAccountId(list) {
      const map = {};
      (Array.isArray(list) ? list : []).forEach(function (b) { map[String(b.accountId)] = +b.balance || 0; });
      return map;
    }
    return Promise.all([
      balance.allAccounts(period),
      balance.allAccounts(prevPeriod),
    ]).then(function (res) {
      const closingByAccount = byAccountId(res[0]);
      const openingByAccount = byAccountId(res[1]);
      return checkings().map(function (a) {
        const id = String(a.id);
        return {
          accountId: a.id,
          closingBalance: id in closingByAccount ? closingByAccount[id] : window.Domain.Account.currentBalance(a),
          openingBalance: id in openingByAccount ? openingByAccount[id] : (+a.balance || 0),
        };
      });
    });
  }

  window.App = window.App || {};
  window.App.StatementService = {
    init: init,
    load: load,
    summary: summary,
  };
})();

/* pages/statement.js — Extrato de Contas.
 * Layout 280px / 1fr: lista de contas (esquerda) + Card de lançamentos com saldo corrente (direita).
 * Conta selecionável; período navegável (mês/ano). App.StatementService monta os itens no cliente
 * (transações do período + snapshot de saldo mensal): StatementItem[] = { date, description, amount,
 * status, runningBal, categoryId? }. A coluna da esquerda usa App.StatementService.summary.
 * Coluna fixa categoria/subcategoria (largura = maior label possível) entre data e descrição.
 * Status 'balance' => linha "Saldo anterior" (sem amount, runningBal = saldo de abertura).
 * Compra de cartão não aparece linha a linha nem cartão a cartão: App.StatementService a colapsa
 * numa linha por (conta, vencimento) — 'Cartões de crédito', lançada na data de vencimento (mesmo
 * mês em que o backend a debita) e clicável para #/credit-cards. Linha de cartões é derivada: sem
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
    return window.TransactionsApi.list('dateFrom=' + b.from + '&dateTo=' + b.to)
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
    const $header = window.pageHeader({
      title: 'Extrato de Contas',
      nav: window.periodNav({
        month: state.month,
        year: state.year,
        onPrev: function () { window.shiftMonth(state, -1, true); reloadPeriod(); },
        onNext: function () { window.shiftMonth(state, +1, true); reloadPeriod(); },
        onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m; state.year = y; reloadPeriod(); },
      })
    });

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
        $left.append(window.selectorButtonHtml({
          id: a.id, active: active, title: a.name, value: fmt(bal), valueColor: window.valueColor(bal),
          cls: 'stm', act: 'select-account',
        }));
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
        const actionsHtml = (isBalance || tx.invoice) ? '' : window.rowActionsHtml(tx.id);

        // O link é um <a href> de verdade: o router é hash-based, então a navegação não precisa
        // de handler (e o deep-link fica copiável/abrível em nova aba).
        const descText = window.Domain.Transaction.describe(tx) || '—';
        const descHtml = tx.invoice
          ? '<a href="#/credit-cards" style="' + descStyle +
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
      window.TransactionsApi.openEditor({
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
      window.TransactionsApi.openDeleteFlow(tx, { list: state.txIndex, onDone: reloadPeriod });
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

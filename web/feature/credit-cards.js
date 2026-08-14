/* feature/credit-cards.js — fatia Cartões de Crédito (mesma f003 do backend, extraída de
 * accounts por ter crescido — cartão é entidade filha da conta, não conta em si). Um arquivo
 * por fatia: domain → application → infrastructure/primary (tela Cartões + Extrato do Cartão),
 * cada bloco abaixo é um IIFE independente (comentário original de cada arquivo preservado como
 * separador de seção). Sem secondary própria: lê via TransactionsApi (fatia irmã) — fecha V1/V2. */

/* domain — credit card rules (uses Account + Period). Pure. */
(function () {
  const DEFAULT_CLOSING_DAY = 1;
  const DEFAULT_DUE_DAY     = 10;

  function pad2(n) { return n < 10 ? '0' + n : '' + n; }

  /* Closing/due day are configured once per account (MON_ACCOUNT_LIMIT) and
     shared by every card on it — so these read from the account, not the card. */
  function closingDay(account) {
    const d = account && +account.closingDay;
    return d > 0 ? d : DEFAULT_CLOSING_DAY;
  }

  function dueDay(account) {
    const d = account && +account.dueDay;
    return d > 0 ? d : DEFAULT_DUE_DAY;
  }

  /* Janela de compras da fatura que VENCE em `period` — delega o ciclo a Domain.Invoice
     (contrato em docs/backend/invoice-cycle.md). Não é o mês-calendário: com closingDay=20 e
     dueDay=5, a fatura que vence em 05/04 cobre 21/02–20/03. Conta sem vencimento no período
     devolve uma janela vazia (from > to), o que zera os totais abaixo. */
  function invoicePeriod(account, period) {
    const dues = window.Domain.Invoice.dueDatesIn(account, period);
    if (!dues.length) return { from: '9999-12-31', to: '0000-01-01' };
    return window.Domain.Invoice.cycleFor(account, dues[0]);
  }

  function usagePct(used, limit) {
    const u = Math.abs(+used || 0);
    const l = +limit || 0;
    if (l <= 0) return 0;
    return Math.min(100, Math.max(0, (u / l) * 100));
  }

  function availableCredit(limit, used) {
    return Math.max(0, (+limit || 0) - Math.abs(+used || 0));
  }

  /* Bar color tokens by usage percent. Mirrors STYLE.md §11. */
  function barColorByUsage(pct) {
    if (pct >= 80) return 'expense';
    if (pct >= 60) return 'warning';
    return 'accent';
  }

  /* Total invoice = sum of |amount| over EXPENSE transactions inside the invoice cycle
     posted against this card (matched by `tx.cardId`, not the account). O ciclo é da conta,
     por isso ela entra na assinatura. */
  function invoiceTotal(transactions, cardId, account, period) {
    const b = invoicePeriod(account, period);
    const cid = String(cardId);
    return (transactions || []).reduce(function (acc, t) {
      if (String(t.cardId) !== cid) return acc;
      const dStr = String(t.date || '').slice(0, 10);
      if (dStr < b.from || dStr > b.to) return acc;
      const isExpense = String(t.type || '').toUpperCase() === 'EXPENSE' || (+t.amount || 0) < 0;
      if (!isExpense) return acc;
      return acc + Math.abs(+t.amount || 0);
    }, 0);
  }

  /* Total invoice for every card on the account combined — used for the shared
     usage bar (account.creditLimit is one limit for all of the account's cards). */
  function accountInvoiceTotal(transactions, account, period) {
    const b = invoicePeriod(account, period);
    const aid = String(account && account.id);
    return (transactions || []).reduce(function (acc, t) {
      if (String(t.accountId) !== aid || t.cardId == null) return acc;
      const dStr = String(t.date || '').slice(0, 10);
      if (dStr < b.from || dStr > b.to) return acc;
      const isExpense = String(t.type || '').toUpperCase() === 'EXPENSE' || (+t.amount || 0) < 0;
      if (!isExpense) return acc;
      return acc + Math.abs(+t.amount || 0);
    }, 0);
  }

  window.Domain = window.Domain || {};
  window.Domain.CreditCard = {
    DEFAULT_CLOSING_DAY: DEFAULT_CLOSING_DAY,
    DEFAULT_DUE_DAY:     DEFAULT_DUE_DAY,
    closingDay:          closingDay,
    dueDay:              dueDay,
    invoicePeriod:       invoicePeriod,
    usagePct:            usagePct,
    availableCredit:     availableCredit,
    barColorByUsage:     barColorByUsage,
    invoiceTotal:        invoiceTotal,
    accountInvoiceTotal: accountInvoiceTotal,
  };
})();

/* _2_application/credit-card-service.js — Credit card use cases. */
(function () {
  let txRepo = null;
  let cache = null;

  function init(deps) {
    txRepo = deps.txRepo;
    cache  = deps.cache;
    return { ready: true };
  }

  /* Every card across every account, flattened, each carrying its owning account
     (for color/name/limit) — used by views that group cards by account client-side. */
  function listFromCache() {
    const out = [];
    cache.accounts().forEach(function (a) {
      (a.cards || []).forEach(function (c) {
        out.push(Object.assign({}, c, { account: a }));
      });
    });
    return out;
  }

  /* Accounts that have at least one card — used by views that render one row/bar
     per account rather than per card (e.g. the dashboard panel). */
  function accountsWithCards() {
    return cache.accounts().filter(window.Domain.Account.hasCards);
  }

  /* Fatura que VENCE em `period`: as compras do ciclo (que começa no mês anterior — ver
     Domain.Invoice) lançadas contra este cartão. Devolve também o vencimento e o ciclo, que a
     tela de Extrato do Cartão usa no cabeçalho. Período sem vencimento → fatura vazia. */
  function invoiceFor(card, account, period) {
    const dues = window.Domain.Invoice.dueDatesIn(account, period);
    if (!dues.length) {
      return Promise.resolve({ dueDate: null, cycle: null, transactions: [], total: 0 });
    }
    const due = dues[0];
    const cycle = window.Domain.Invoice.cycleFor(account, due);
    return txRepo.listByAccount(account.id, 'dateFrom=' + cycle.from + '&dateTo=' + cycle.to)
      .then(function (txs) {
        const items = (Array.isArray(txs) ? txs : []).filter(function (t) {
          return String(t.cardId) === String(card.id);
        });
        return {
          dueDate: due,
          cycle: cycle,
          transactions: items,
          total: window.Domain.CreditCard.invoiceTotal(items, card.id, account, period),
        };
      });
  }

  window.App = window.App || {};
  window.App.CreditCardService = {
    init: init,
    listFromCache: listFromCache,
    accountsWithCards: accountsWithCards,
    invoiceFor: invoiceFor,
  };
})();

/* pages/credit-cards.js — Cartões de Crédito.
 *
 * Cartões não são mais contas — cada um é `{ id, last4, accountId, active }` filho
 * de uma conta real. A página agrupa os cartões do cache por conta (uma "conta com
 * cartões" pode ter 1+ cartões) e renderiza um tile por grupo:
 *   - banda visual top em gradient com a cor/nome DA CONTA
 *   - uma linha por cartão (last4 + fatura daquele cartão, casada por tx.cardId)
 *   - barra de uso COMPARTILHADA (uma por grupo): total dos cartões da conta vs
 *     account.creditLimit — o limite é da conta, não de cada cartão
 *   - rodapé Fechamento/Vencimento (também da conta)
 *
 * Período: navegação por mês, pelo VENCIMENTO da fatura — o ciclo vem de
 * Domain.CreditCard.invoicePeriod (closingDay/dueDay da conta, ver Domain.Invoice), não do mês
 * calendário. Backend GET /api/transactions ignora filtros accountId/from/to, então buscamos a
 * lista completa uma vez e particionamos por cartão + intervalo de datas no cliente.
 * "Ver fatura" leva para #/card-statement/{cardId} — o modal de fatura virou tela própria.
 *
 * "Novo Cartão" → redireciona para #/accounts (cartões são geridos no modal de
 * edição da conta).
 */
(function () {
  window.Pages = window.Pages || {};

  // ── State ─────────────────────────────────────────────────
  let state = null;

  function resetState() {
    const p = window.App.PeriodService.get(); // { month: 1-12, year }
    state = {
      $root: null,
      groups: [], // [{ account, cards: [...] }]
      month: p.month, // 1..12
      year: p.year,
      allTx: [],
      txLoading: true,
    };
  }

  // Cards live in App.CacheStore (hydrated at login, SSE-refreshed), grouped by
  // their owning account for the grid.
  function syncGroupsFromCache() {
    const flat = window.App.CreditCardService.listFromCache();
    const byAccount = {};
    flat.forEach(function (c) {
      const aid = String(c.accountId);
      if (!byAccount[aid]) byAccount[aid] = { account: c.account, cards: [] };
      byAccount[aid].cards.push(c);
    });
    const groups = Object.keys(byAccount).map(function (k) { return byAccount[k]; });
    groups.forEach(function (g) {
      g.cards.sort(function (a, b) { return String(a.last4).localeCompare(String(b.last4)); });
    });
    groups.sort(function (a, b) { return window.sortByName(a.account, b.account); });
    state.groups = groups;
  }

  // ── Helpers ───────────────────────────────────────────────

  function currentPeriod() {
    return window.Domain.Period.create(state.month, state.year);
  }

  function pctColor(pct) {
    return 'var(--' + window.Domain.CreditCard.barColorByUsage(pct) + ')';
  }

  function loadAllTx() {
    state.txLoading = true;
    return window.TransactionsApi.list('limit=5000&sort=date,desc').then(function (list) {
      state.allTx = Array.isArray(list) ? list : [];
      state.txLoading = false;
      render();
    }).catch(function () {
      state.allTx = [];
      state.txLoading = false;
      render();
    });
  }

  // Invoice for a single card (matched by tx.cardId) — used per row.
  function computeInvoice(account, cardId) {
    const period = currentPeriod();
    const b = window.Domain.CreditCard.invoicePeriod(account, period);
    const cid = String(cardId);
    const items = (state.allTx || []).filter(function (t) {
      if (String(t.cardId) !== cid) return false;
      const d = String(t.date || '').slice(0, 10);
      return d >= b.from && d <= b.to;
    });
    const total = window.Domain.CreditCard.invoiceTotal(items, cardId, account, period);
    return { items: items, total: total };
  }

  // Combined invoice for every card on the account — feeds the shared usage bar.
  function computeAccountInvoice(account) {
    return window.Domain.CreditCard.accountInvoiceTotal(state.allTx || [], account, currentPeriod());
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $page = $('<div class="fade-in"></div>');

    // Header
    const $header = $(
      '<div class="page-header">' +
        '<h1>Cartões de Crédito</h1>' +
        '<div class="page-header-actions" data-region="head-actions"></div>' +
      '</div>'
    );

    const $actions = $header.find('[data-region=head-actions]');
    const $periodNav = window.periodNav({
      month: state.month,
      year: state.year,
      onPrev: function () { window.shiftMonth(state, -1, true); render(); },
      onNext: function () { window.shiftMonth(state, +1, true); render(); },
      onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m; state.year = y; render(); },
    });
    $actions.append($periodNav);
    // $actions.append(window.btn({
    //   variant: 'primary', size: 'md', icon: 'plus', label: 'Novo Cartão',
    //   attrs: 'data-act="new"'
    // }));
    $page.append($header);

    // Body
    if (!state.groups.length) {
      $page.append(window.emptyState({
        icon: 'creditCard',
        title: 'Nenhum cartão cadastrado',
        desc: 'Adicione cartões na tela de Contas Bancárias.'
      }));
    } else {
      const $grid = $(
        '<div style="display:grid;gap:16px;' +
          'grid-template-columns:repeat(auto-fill, minmax(320px, 1fr));"></div>'
      );
      state.groups.forEach(function (g) { $grid.append(renderGroup(g)); });
      $page.append($grid);
    }

    $root.empty().append($page);
  }

  function renderGroup(group) {
    const account = group.account;
    const color = account.color || '#820AD1';
    const closingDay = window.Domain.CreditCard.closingDay(account);
    const dueDay = window.Domain.CreditCard.dueDay(account);
    const limit = Number(account.creditLimit) || 0;

    const accountUsed = computeAccountInvoice(account);
    const pct = window.Domain.CreditCard.usagePct(accountUsed, limit);
    const barColor = pctColor(pct);
    const available = window.Domain.CreditCard.availableCredit(limit, accountUsed);

    const $card = $(
      '<div class="card cc" data-account-id="' + esc(account.id) + '" style="' +
        'padding:20px;display:flex;flex-direction:column;gap:14px;' +
        'transition:border-color var(--transition);' +
      '"></div>'
    );

    // ── Visual band (gradient) — cor/nome da conta ─────────
    const bandStyle =
      'position:relative;overflow:hidden;min-height:120px;' +
      'border-radius:var(--radius);padding:20px;color:#fff;' +
      'background:linear-gradient(135deg, ' + esc(color) + ', ' + esc(color) + '99);';

    const $header = $('<div style="' + bandStyle + '"></div>');
    $header.append(
      '<div style="position:absolute;right:-32px;top:-32px;width:160px;height:160px;' +
        'border-radius:50%;background:rgba(255,255,255,0.10);"></div>'
    );
    $header.append(
      '<div style="display:flex;align-items:center;gap:8px;' +
        'font-size:11px;font-weight:700;letter-spacing:0.12em;opacity:0.75;">' +
        window.icon('creditCard', 14) +
        '<span>CRÉDITO' + (account.active === false ? ' · INATIVA' : '') + '</span>' +
      '</div>'
    );
    $header.append(
      '<div style="font-size:18px;font-weight:800;margin-top:24px;' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
        esc(account.name || '—') +
      '</div>'
    );
    $header.append(
      '<div style="position:absolute;bottom:14px;left:20px;font-size:11px;opacity:0.55;">' +
        'Fecha dia ' + esc(closingDay) +
      '</div>'
    );
    $header.append(
      '<div style="position:absolute;bottom:14px;right:16px;font-size:11px;opacity:0.55;">' +
        'Vence dia ' + esc(dueDay) +
      '</div>'
    );
    $card.append($header);

    // ── Shared usage bar (account-level: all of the account's cards combined) ─
    const $usage = $(
        '<div>' +
        '<div style="display:flex;justify-content:space-between;margin-bottom:4px;">' +
        '<span style="font-size:11px;color:var(--text-muted);font-weight:700;' +
        'text-transform:uppercase;letter-spacing:0.04em;">Limite da conta</span>' +
        '<span style="font-size:12px;font-weight:700;color:var(--text-primary);">' + esc(fmt(limit)) + '</span>' +
        '</div>' +
        '<div style="height:8px;background:var(--bg-hover);border-radius:4px;overflow:hidden;">' +
        '<div style="height:100%;border-radius:4px;width:' + pct.toFixed(1) + '%;' +
        'background:' + barColor + ';transition:width 0.5s ease;"></div>' +
        '</div>' +
        '<div style="display:flex;justify-content:space-between;margin-top:6px;' +
        'font-size:12px;color:var(--text-muted);">' +
        '<span>' + esc(pct.toFixed(0)) + '% utilizado</span>' +
        '<span>' + esc(fmt(available)) + ' disponível</span>' +
        '</div>' +
        '</div>'
    );
    $card.append($usage);

    // ── Per-card rows: last4 + fatura do cartão + Ver fatura ─
    const $rows = $('<div style="display:flex;flex-direction:column;"></div>');
    group.cards.forEach(function (c) {
      const inv = computeInvoice(account, c.id);
      const used = Number(inv.total) || 0;
      const valueHtml = state.txLoading
        ? '<span style="font-size:13px;color:var(--text-muted);">…</span>'
        : '<span style="font-size:14px;font-weight:800;color:' +
            (used > 0 ? 'var(--expense)' : 'var(--text-primary)') + ';">' + esc(fmt(used)) + '</span>';
      $rows.append(
        '<div style="display:flex;justify-content:space-between;align-items:center;' +
          'padding:9px 0;border-bottom:1px solid var(--border-light);">' +
          '<span style="font-size:12px;color:var(--text-secondary);letter-spacing:0.04em;">' +
            '•••• ' + esc(c.last4) +
          '</span>' +
          valueHtml +
          '<a href="#/card-statement/' + esc(c.id) + '" ' +
            'style="text-decoration:none;font-size:12px;' +
            'font-weight:600;color:var(--accent);display:inline-flex;align-items:center;gap:4px;">' +
            'Ver fatura ' + window.icon('chevronRight', 12) +
          '</a>' +
        '</div>'
      );
    });
    $card.append($rows);

    $card.on('mouseenter', function () { $card.css('border-color', color); });
    $card.on('mouseleave', function () { $card.css('border-color', ''); });

    return $card;
  }

  // ── Event delegation ──────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.cc', '[data-act=new]', function () {
      window.location.hash = '#/accounts';
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['credit-cards'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      syncGroupsFromCache();
      render();
      loadAllTx();
      state.unsubscribe = window.AccountsApi.onChange(function () {
        syncGroupsFromCache();
        render();
      });
    },
    unmount: function () {
      if (state && state.$root) state.$root.off('.cc');
      if (state && state.unsubscribe) state.unsubscribe();
      state = null;
    }
  };
})();

/* pages/card-statement.js — Extrato do Cartão (#/card-statement/{cardId}).
 * Mesmo leiaute do Extrato de Contas (pages/statement.js): grid 280px / 1fr, lista de cartões à
 * esquerda + card de lançamentos com acumulado à direita, linhas .stm-row com as mesmas colunas
 * (data | categoria | descrição | valor | acumulado | dot de status | ações).
 *
 * Duas diferenças de conteúdo, ambas por ser fatura e não conta:
 *   - o período navegado é o do VENCIMENTO: o ciclo de compras vem de Domain.Invoice.cycleFor e
 *     começa no mês anterior (com closingDay=20/dueDay=5, a fatura que vence em 05/04 cobre
 *     21/02–20/03);
 *   - a linha-cabeçalho é "Fatura anterior" — traz o total do ciclo anterior na coluna de saldo,
 *     mas o acumulado do ciclo atual recomeça em 0, então a última linha é o total da fatura.
 *
 * Fora do sidebar: chega-se aqui pelo link da linha de fatura (Lançamentos/Extrato de Contas) ou
 * pelo "Ver fatura" da tela de Cartões.
 */
(function () {
  window.Pages = window.Pages || {};

  let state = null;

  function resetState(cardId) {
    const p = window.App.PeriodService.get(); // { month: 1-12, year }
    state = {
      $root: null,
      cardId: cardId ? String(cardId) : null,
      month: p.month, // 1-12
      year: p.year,
      items: [],
      totals: {},   // cardId -> total da fatura do período (coluna da esquerda)
      cycle: null,  // { from, to } do cartão selecionado
      dueDate: null,
      txIndex: [],  // transações cruas do ciclo — resolve editar/excluir
      loading: false,
    };
  }

  // ── Helpers ───────────────────────────────────────────────

  function cards() {
    return window.App.CreditCardService.listFromCache().slice().sort(function (a, b) {
      const byAccount = window.sortByName(a.account, b.account);
      return byAccount !== 0 ? byAccount : String(a.last4).localeCompare(String(b.last4));
    });
  }

  function selectedCard() {
    return window.byId(cards(), state.cardId);
  }

  function currentPeriod() {
    return window.Domain.Period.create(state.month, state.year);
  }

  function findFullTx(id) {
    return window.byId(state.txIndex, id);
  }

  // ── Fetch ─────────────────────────────────────────────────

  /* Fatura do cartão selecionado: ciclo atual (linhas + acumulado) e o total do ciclo anterior,
     que vira a linha-cabeçalho "Fatura anterior". */
  function loadInvoice() {
    const card = selectedCard();
    if (!card) {
      state.items = [];
      state.loading = false;
      render();
      return Promise.resolve();
    }
    state.loading = true;
    render();

    const period = currentPeriod();
    const previous = window.Domain.Period.shift(period, -1);
    return Promise.all([
      window.App.CreditCardService.invoiceFor(card, card.account, period),
      window.App.CreditCardService.invoiceFor(card, card.account, previous),
    ]).then(function (res) {
      const invoice = res[0];
      const openingLabel = invoice.cycle ? invoice.cycle.from : null;
      state.cycle = invoice.cycle;
      state.dueDate = invoice.dueDate;
      state.txIndex = invoice.transactions;
      state.items = window.Domain.StatementItem.buildRows(0, invoice.transactions, openingLabel, {
        headerLabel: 'Fatura anterior',
        headerBalance: -Math.abs(res[1].total || 0),
        startBalance: 0,
      });
      state.loading = false;
      render();
    }).catch(function (err) {
      state.items = [];
      state.loading = false;
      render();
      window.toast((err && err.message) || 'Falha ao carregar a fatura', 'error');
    });
  }

  /* Totais de todos os cartões no período — alimenta a coluna da esquerda (paralelo ao panorama
     por conta do Extrato de Contas). Uma requisição por cartão, como o extrato faz por conta. */
  function loadTotals() {
    const list = cards();
    if (!list.length) return Promise.resolve();
    return Promise.all(list.map(function (c) {
      return window.App.CreditCardService.invoiceFor(c, c.account, currentPeriod())
        .then(function (inv) { return { id: c.id, total: inv.total || 0 }; })
        .catch(function () { return { id: c.id, total: 0 }; });
    })).then(function (res) {
      const map = {};
      res.forEach(function (r) { map[String(r.id)] = r.total; });
      state.totals = map;
      render();
    });
  }

  function reloadPeriod() {
    return Promise.all([loadTotals(), loadInvoice()]);
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $page = $('<div class="fade-in"></div>');
    const card = selectedCard();

    // Header — sticky, igual ao extrato de contas.
    const cycleLabel = state.cycle
      ? 'Compras de ' + fmtDate(state.cycle.from) + ' a ' + fmtDate(state.cycle.to) +
        (state.dueDate ? ' · vence ' + fmtDate(state.dueDate) : '')
      : '';
    const $header = $(
      '<div class="page-header">' +
        '<div>' +
          '<h1>Extrato do Cartão</h1>' +
          (cycleLabel
            ? '<p style="font-size:12px;color:var(--text-muted);margin-top:2px;">' +
                esc(cycleLabel) +
              '</p>'
            : '') +
        '</div>' +
        '<div class="page-header-actions" data-region="head-actions"></div>' +
      '</div>'
    );
    const $actions = $header.find('[data-region=head-actions]');
    $actions.append(window.periodNav({
      month: state.month,
      year: state.year,
      onPrev: function () { window.shiftMonth(state, -1, true); reloadPeriod(); },
      onNext: function () { window.shiftMonth(state, +1, true); reloadPeriod(); },
      onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m; state.year = y; reloadPeriod(); },
    }));
    $actions.append(window.btn({
      variant: 'secondary', size: 'md', icon: 'creditCard', label: 'Cartões',
      attrs: 'data-act="back"'
    }));

    const $sticky = $('<div style="position:sticky;top:0;z-index:5;background:var(--bg-base);"></div>');
    $sticky.append($header);
    $page.append($sticky);

    const $grid = $(
      '<div style="display:grid;grid-template-columns:280px 1fr;gap:16px;' +
        'height:calc(100vh - 160px);min-height:280px;"></div>'
    );

    // Left column: cards list (mesmo botão da lista de contas do extrato).
    const $left = $('<div style="display:flex;flex-direction:column;gap:12px;overflow-y:auto;"></div>');
    const list = cards();
    if (list.length === 0) {
      $left.append(
        '<div style="font-size:13px;color:var(--text-muted);padding:14px 16px;' +
        'background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius);">' +
          'Nenhum cartão cadastrado.' +
        '</div>'
      );
    } else {
      list.forEach(function (c) {
        const active = String(c.id) === String(state.cardId);
        const total = state.totals[String(c.id)] || 0;
        const btnStyle =
          'padding:14px 16px;border-radius:var(--radius);text-align:left;' +
          'background:' + (active ? 'var(--accent-light)' : 'var(--bg-card)') + ';' +
          'border:1px solid ' + (active ? 'var(--accent)' : 'var(--border)') + ';' +
          'color:' + (active ? 'var(--accent)' : 'var(--text-primary)') + ';' +
          'cursor:pointer;font-weight:' + (active ? '700' : '400') + ';font-size:13px;' +
          'transition:all var(--transition);display:flex;flex-direction:column;gap:4px;';
        $left.append(
          '<button type="button" class="cst" data-act="select-card" ' +
            'data-id="' + esc(c.id) + '" style="' + btnStyle + '">' +
            '<span>' + esc((c.account && c.account.name) || '—') + ' •••• ' + esc(c.last4) + '</span>' +
            '<span style="font-size:11px;color:' + (total > 0 ? 'var(--expense)' : 'var(--text-muted)') + ';' +
              'font-weight:' + (active ? '700' : '500') + ';">' + esc(fmt(total)) + '</span>' +
          '</button>'
        );
      });
    }
    $grid.append($left);

    // Right column: invoice card.
    const $card = $('<div class="card" style="padding:0;overflow-y:auto;"></div>');

    if (!card) {
      $card.append(window.emptyState({
        icon: 'creditCard',
        title: 'Selecione um cartão',
        desc: 'Escolha um cartão à esquerda para visualizar a fatura.'
      }));
    } else if (state.loading) {
      $card.append(window.emptyState({ icon: 'creditCard', title: 'Carregando…' }));
    } else if (state.items.length <= 1) {
      $card.append(window.emptyState({
        icon: 'creditCard',
        title: 'Sem lançamentos nesta fatura',
        desc: 'Tente outro mês ou selecione outro cartão.'
      }));
    } else {
      renderRows($card);
    }

    $grid.append($card);
    $page.append($grid);

    if (card && !state.loading) {
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

  /* Linha idêntica à do extrato de conta — a coluna de saldo carrega o acumulado da fatura. */
  function renderRows($card) {
    const items = state.items;
    const catMap = window.categoryById();
    const catLens = window.flatCategories().map(function (c) { return c.label.length; });
    const catColCh = (catLens.length ? Math.max.apply(null, catLens) : 12) + 1;

    items.forEach(function (tx, i) {
      const isLast = i === items.length - 1;
      const isHeader = window.Domain.StatementItem.isBalanceHeader(tx);
      const cat = isHeader ? null : catMap[tx.categoryId];
      const catLbl = cat ? window.categoryLabel(cat) : '';
      const amt = Number(tx.amount) || 0;
      const dotColor =
        tx.status === 'confirmed' ? 'var(--income)' :
        isHeader ? 'var(--text-muted)' : 'var(--warning)';
      const runningBal = window.Domain.StatementItem.runningBalance(tx);

      const rowStyle =
        'display:flex;align-items:center;gap:16px;padding:11px 20px;' +
        (isLast ? '' : 'border-bottom:1px solid var(--border-light);') +
        'background:' + (isHeader ? 'var(--bg-hover)' : 'transparent') + ';' +
        'transition:background var(--transition);';

      const descStyle =
        'flex:1;font-size:13px;font-weight:' + (isHeader ? '700' : '500') + ';' +
        'color:' + (isHeader ? 'var(--text-secondary)' : 'var(--text-primary)') + ';' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-transform:uppercase;';

      const catStyle =
        'flex:0 0 ' + catColCh + 'ch;width:' + catColCh + 'ch;' +
        'font-size:12px;color:var(--text-muted);' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

      const amountHtml = (!isHeader && amt !== 0)
        ? '<span style="font-size:13px;font-weight:700;color:' + window.valueColor(amt) + ';">' +
            esc(fmt(amt)) +
          '</span>'
        : '';

      const actionsHtml = isHeader ? '' :
        '<button type="button" class="icon-btn" title="Editar" ' +
          'data-act="edit" data-id="' + esc(tx.id) + '" style="width:28px;height:28px;">' +
          window.icon('edit', 14) +
        '</button>' +
        '<button type="button" class="icon-btn" title="Excluir" ' +
          'data-act="trash" data-id="' + esc(tx.id) + '" style="width:28px;height:28px;color:var(--expense);">' +
          window.icon('trash', 14) +
        '</button>';

      $card.append(
        '<div class="stm-row" data-id="' + esc(tx.id || '') + '" style="' + rowStyle + '">' +
          '<span style="font-size:12px;color:var(--text-muted);min-width:56px;">' +
            esc(tx.date ? fmtDate(tx.date) : '') +
          '</span>' +
          '<span style="' + catStyle + '">' + esc(catLbl) + '</span>' +
          window.tagFlagHtml(tx.tagIds) +
          '<span style="' + descStyle + '">' +
            esc(window.Domain.Transaction.describe(tx) || '—') +
          '</span>' +
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

  // ── Event delegation ──────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.cst', '[data-act=back]', function () {
      window.location.hash = '#/credit-cards';
    });

    // Trocar de cartão reescreve a rota: o hash é o estado da tela (deep-link compartilhável).
    $root.on('click.cst', '[data-act=select-card]', function () {
      const id = $(this).attr('data-id');
      if (id && String(id) !== String(state.cardId)) {
        window.location.hash = '#/card-statement/' + id;
      }
    });

    $root.on('click.cst', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const tx = findFullTx($(this).attr('data-id'));
      if (!tx) { window.toast('Lançamento indisponível — recarregue o período', 'error'); return; }
      window.TransactionsApi.openEditor({
        existing: tx,
        list: state.txIndex,
        defaultDate: state.cycle ? state.cycle.to : null,
        onSaved: reloadPeriod,
      });
    });
    $root.on('click.cst', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const tx = findFullTx($(this).attr('data-id'));
      if (!tx) { window.toast('Lançamento indisponível — recarregue o período', 'error'); return; }
      window.TransactionsApi.openDeleteFlow(tx, { list: state.txIndex, onDone: reloadPeriod });
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['card-statement'] = {
    mount: function ($root, cardId) {
      resetState(cardId);
      state.$root = $root;
      bindRoot($root);

      // Sem cartão no path (ou cartão inexistente): cai no primeiro disponível.
      if (!selectedCard()) {
        const list = cards();
        state.cardId = list.length ? String(list[0].id) : null;
      }
      render();
      reloadPeriod();
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.cst');
      }
      state = null;
    }
  };
})();

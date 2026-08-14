/* feature/credit-cards.js — fatia Cartões de Crédito (mesma f003 do backend, extraída de
 * accounts por ter crescido — cartão é entidade filha da conta, não conta em si): domain →
 * application → infrastructure/primary (tela Cartões), cada bloco abaixo é um IIFE independente
 * (comentário original de cada arquivo preservado como separador de seção). A tela Extrato do
 * Cartão vive em feature/card-statement.js (mesma fatia, lê Domain.CreditCard e
 * App.CreditCardService daqui). Sem secondary própria: lê via TransactionsApi (fatia irmã) —
 * fecha V1/V2. */

/* domain — credit card rules (uses Account + Period). Pure. */
(function () {
  const DEFAULT_CLOSING_DAY = 1;
  const DEFAULT_DUE_DAY     = 10;

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

  /* Bar color tokens by usage percent. Mirrors STYLE.md §11 (kernel-shared threshold). */
  function barColorByUsage(pct) {
    return window.thresholdColorToken(pct);
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
    const $header = window.pageHeader({
      title: 'Cartões de Crédito',
      nav: window.periodNav({
        month: state.month,
        year: state.year,
        onPrev: function () { window.shiftMonth(state, -1, true); render(); },
        onNext: function () { window.shiftMonth(state, +1, true); render(); },
        onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m; state.year = y; render(); },
      })
    });
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
        window.progressBarHtml(pct.toFixed(1), barColor) +
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

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
 * Período: navegação por mês (periodNav). Backend GET /api/transactions ignora
 * filtros accountId/from/to, então buscamos a lista completa uma vez e
 * particionamos por cartão + intervalo de datas no cliente.
 *
 * "Novo Cartão" → redireciona para #/accounts (cartões são geridos no modal de
 * edição da conta).
 */
(function () {
  window.Pages = window.Pages || {};

  // ── State ─────────────────────────────────────────────────
  let state = null;

  function resetState() {
    const now = new Date();
    state = {
      $root: null,
      groups: [], // [{ account, cards: [...] }]
      month: now.getMonth() + 1, // 1..12
      year: now.getFullYear(),
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

  function findCard(id) {
    for (let i = 0; i < state.groups.length; i++) {
      const found = window.byId(state.groups[i].cards, id);
      if (found) return found;
    }
    return null;
  }

  function pctColor(pct) {
    return 'var(--' + window.Domain.CreditCard.barColorByUsage(pct) + ')';
  }

  function loadAllTx() {
    state.txLoading = true;
    return window.App.TransactionService.list('limit=5000&sort=date,desc').then(function (list) {
      state.allTx = Array.isArray(list) ? list : [];
      state.txLoading = false;
      render();
    }).catch(function () {
      state.allTx = [];
      state.txLoading = false;
      render();
    });
  }

  // Invoice for a single card (matched by tx.cardId) — used per row + the modal.
  function computeInvoice(cardId) {
    const period = currentPeriod();
    const b = window.Domain.CreditCard.invoicePeriod(period);
    const cid = String(cardId);
    const items = (state.allTx || []).filter(function (t) {
      if (String(t.cardId) !== cid) return false;
      const d = String(t.date || '').slice(0, 10);
      return d >= b.from && d <= b.to;
    });
    const total = window.Domain.CreditCard.invoiceTotal(items, cardId, period);
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
      label: window.monthLabel(state.month, state.year),
      onPrev: function () { window.shiftMonth(state, -1, true); render(); },
      onNext: function () { window.shiftMonth(state, +1, true); render(); },
    });
    $actions.append($periodNav);
    $actions.append(window.btn({
      variant: 'primary', size: 'md', icon: 'plus', label: 'Novo Cartão',
      attrs: 'data-act="new"'
    }));
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

    const $band = $('<div style="' + bandStyle + '"></div>');
    $band.append(
      '<div style="position:absolute;right:-32px;top:-32px;width:160px;height:160px;' +
        'border-radius:50%;background:rgba(255,255,255,0.10);"></div>'
    );
    $band.append(
      '<div style="display:flex;align-items:center;gap:8px;' +
        'font-size:11px;font-weight:700;letter-spacing:0.12em;opacity:0.75;">' +
        window.icon('creditCard', 14) +
        '<span>CRÉDITO' + (account.active === false ? ' · INATIVA' : '') + '</span>' +
      '</div>'
    );
    $band.append(
      '<div style="font-size:18px;font-weight:800;margin-top:24px;' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
        esc(account.name || '—') +
      '</div>'
    );
    const last4Line = group.cards.map(function (c) { return '•••• ' + c.last4; }).join('   ');
    $band.append(
      '<div style="font-size:13px;opacity:0.75;margin-top:8px;letter-spacing:0.06em;' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
        esc(last4Line) +
      '</div>'
    );
    $band.append(
      '<div style="position:absolute;bottom:14px;right:16px;font-size:11px;opacity:0.55;">' +
        'Vence dia ' + esc(dueDay) +
      '</div>'
    );
    $card.append($band);

    // ── Per-card rows: last4 + fatura do cartão + Ver fatura ─
    const $rows = $('<div style="display:flex;flex-direction:column;"></div>');
    group.cards.forEach(function (c) {
      const inv = computeInvoice(c.id);
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
          '<button type="button" class="cc" data-act="view-invoice" data-id="' + esc(c.id) + '" ' +
            'style="background:transparent;border:none;cursor:pointer;font-size:12px;' +
            'font-weight:600;color:var(--accent);display:inline-flex;align-items:center;gap:4px;">' +
            'Ver fatura ' + window.icon('chevronRight', 12) +
          '</button>' +
        '</div>'
      );
    });
    $card.append($rows);

    // ── Shared usage bar (account-level: all of the account's cards combined) ─
    const $bar = $(
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
    $card.append($bar);

    // ── Footer: closing/due (conta) ─────────────────────────
    $card.append(
      '<div style="padding-top:2px;">' +
        '<span style="font-size:12px;color:var(--text-muted);">' +
          'Fech. dia ' + esc(closingDay) + ' · Venc. dia ' + esc(dueDay) +
        '</span>' +
      '</div>'
    );

    $card.on('mouseenter', function () { $card.css('border-color', color); });
    $card.on('mouseleave', function () { $card.css('border-color', ''); });

    return $card;
  }

  // ── Invoice modal (por cardId) ─────────────────────────────
  function openInvoiceModal(cardId) {
    const card = findCard(cardId);
    if (!card) return;
    const inv = computeInvoice(cardId);
    const loadingInv = state.txLoading;

    const headerSummary =
      '<div style="display:flex;justify-content:space-between;align-items:center;' +
        'padding:12px 16px;background:var(--bg-hover);border-radius:var(--radius-sm);' +
        'margin-bottom:14px;">' +
        '<div>' +
          '<p style="font-size:11px;color:var(--text-muted);font-weight:700;' +
            'text-transform:uppercase;letter-spacing:0.04em;">Total da fatura</p>' +
          '<p style="font-size:18px;font-weight:800;color:var(--expense);margin-top:2px;">' +
            esc(fmt(inv.total || 0)) +
          '</p>' +
        '</div>' +
        '<div style="text-align:right;font-size:12px;color:var(--text-muted);">' +
          esc(window.monthLabel(state.month, state.year)) +
        '</div>' +
      '</div>';

    let listHtml;
    if (loadingInv) {
      listHtml = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px;">' +
        'Carregando lançamentos…</div>';
    } else if (!inv.items || !inv.items.length) {
      listHtml = window.emptyState({
        icon: 'creditCard',
        title: 'Sem lançamentos neste período',
        desc: 'Tente outro mês.'
      });
    } else {
      // Group by date.
      const byDate = {};
      const order = [];
      inv.items.forEach(function (t) {
        const k = String(t.date || '').slice(0, 10);
        if (!byDate[k]) { byDate[k] = []; order.push(k); }
        byDate[k].push(t);
      });
      order.sort();
      const rows = order.map(function (k) {
        const dayHeader =
          '<div style="font-size:11px;font-weight:700;color:var(--text-muted);' +
            'text-transform:uppercase;letter-spacing:0.04em;padding:10px 16px 6px 16px;">' +
            esc(fmtDate(k)) +
          '</div>';
        const rowsHtml = byDate[k].map(function (t) {
          const amt = Number(t.amount) || 0;
          const isExpense = (t.type === 'EXPENSE') || amt < 0;
          const absAmt = Math.abs(amt);
          const color = isExpense ? 'var(--expense)' : 'var(--income)';
          return '<div style="display:flex;align-items:center;gap:12px;' +
            'padding:10px 16px;border-bottom:1px solid var(--border-light);">' +
            '<span style="flex:1;font-size:13px;color:var(--text-primary);' +
              'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-transform:uppercase;">' +
              esc(t.description || '—') +
            '</span>' +
            '<span style="font-size:13px;font-weight:700;color:' + color + ';">' +
              esc(fmt(absAmt)) +
            '</span>' +
          '</div>';
        }).join('');
        return dayHeader + rowsHtml;
      }).join('');
      listHtml =
        '<div style="border:1px solid var(--border);border-radius:var(--radius);overflow:hidden;">' +
          rows +
        '</div>';
    }

    const $close = window.btn({
      variant: 'secondary', size: 'md', label: 'Fechar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const accountName = card.account ? (card.account.name || '') : '';
    const m = window.modal({
      title: 'Fatura · ' + accountName + ' · •••• ' + (card.last4 || ''),
      body: headerSummary + listHtml,
      footer: window.modalFooter($close),
    });
    m.open();
  }

  // ── Event delegation ──────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.cc', '[data-act=new]', function () {
      window.location.hash = '#/accounts';
    });
    $root.on('click.cc', '[data-act=view-invoice]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      openInvoiceModal(id);
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
      state.unsubscribe = window.App.AccountService.onChange(function () {
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

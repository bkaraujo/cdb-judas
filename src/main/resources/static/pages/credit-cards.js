/* pages/credit-cards.js — Cartões de Crédito.
 *
 * Lista cartões (accounts onde type==='CREDIT_CARD') em grid de "card visuals" com:
 *   - banda visual top em gradient com card.color
 *   - bloco "Fatura atual" (despesas do período no cartão)
 *   - barra de uso (used/limit) com cores 60/80% (STYLE.md §11)
 *   - rodapé Fechamento/Vencimento + link "Ver fatura"
 *
 * Período: navegação por mês (periodNav). Backend GET /api/transactions ignora
 * filtros accountId/from/to, então buscamos a lista completa uma vez e
 * particionamos por cartão + intervalo de datas no cliente.
 *
 * "Novo Cartão" → redireciona para #/accounts (criação acontece lá com type=CREDIT_CARD).
 */
(function () {
  window.Pages = window.Pages || {};

  // ── State ─────────────────────────────────────────────────
  let state = null;

  function resetState() {
    const now = new Date();
    state = {
      $root: null,
      cards: [],
      month: now.getMonth() + 1, // 1..12
      year: now.getFullYear(),
      allTx: [],
      txLoading: true,
    };
  }

  // Cards live in App.CacheStore (hydrated at login, SSE-refreshed).
  function syncCardsFromCache() {
    state.cards = window.App.CreditCardService.listFromCache();
  }

  // ── Helpers ───────────────────────────────────────────────



  function currentPeriod() {
    return window.Domain.Period.create(state.month, state.year);
  }

  function findCard(id) {
    for (let i = 0; i < state.cards.length; i++) {
      if (String(state.cards[i].id) === String(id)) return state.cards[i];
    }
    return null;
  }

  function accountName(id) {
    if (!id) return '—';
    const acc = window.App.CacheStore.findById('accounts', id);
    return acc ? (acc.name || '—') : '—';
  }

  function colorOf(c) { return c.color || '#820AD1'; }

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

  // Filter transactions by card account + month/year. Returns { items, total }.
  // Backend list endpoint ignores accountId/from/to filters, so partition client-side.
  function computeInvoice(cardId) {
    const period = currentPeriod();
    const b = window.Domain.CreditCard.invoicePeriod(period);
    const cid = String(cardId);
    const items = (state.allTx || []).filter(function (t) {
      if (String(t.accountId) !== cid) return false;
      const d = String(t.date || '').slice(0, 10);
      return d >= b.from && d <= b.to;
    });
    const total = window.Domain.CreditCard.invoiceTotal(items, cardId, period);
    return { items: items, total: total };
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
      onPrev: function () {
        if (--state.month < 1) { state.month = 12; state.year--; }
        render();
      },
      onNext: function () {
        if (++state.month > 12) { state.month = 1; state.year++; }
        render();
      },
    });
    $actions.append($periodNav);
    $actions.append(window.btn({
      variant: 'primary', size: 'md', icon: 'plus', label: 'Novo Cartão',
      attrs: 'data-act="new"'
    }));
    $page.append($header);

    // Body
    if (!state.cards.length) {
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
      state.cards.slice().sort(window.sortByName).forEach(function (c) {
        $grid.append(renderCard(c));
      });
      $page.append($grid);
    }

    $root.empty().append($page);
  }

  function renderCard(c) {
    const info = c.additionalInfo || {};
    const color = colorOf(c);
    const last4 = info.last4 || '0000';
    const closingDay = window.Domain.CreditCard.closingDay(c);
    const dueDay = window.Domain.CreditCard.dueDay(c);
    const limit = Number(info.limit) || 0;

    const inv = computeInvoice(c.id);
    const loadingInv = state.txLoading;
    const used = Number(inv.total) || 0;
    const pct = window.Domain.CreditCard.usagePct(used, limit);
    const barColor = pctColor(pct);
    const available = window.Domain.CreditCard.availableCredit(limit, used);

    const linkedName = accountName(c.linkedAccountId);

    const $card = $(
      '<div class="card cc" data-id="' + esc(c.id) + '" style="' +
        'padding:20px;display:flex;flex-direction:column;gap:16px;' +
        'cursor:pointer;transition:border-color var(--transition);' +
      '"></div>'
    );

    // ── Visual band (gradient) ─────────────────────────────
    const bandStyle =
      'position:relative;overflow:hidden;min-height:140px;' +
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
        '<span>CRÉDITO' + (c.active === false ? ' · INATIVO' : '') + '</span>' +
      '</div>'
    );
    $band.append(
      '<div style="font-size:18px;font-weight:800;margin-top:24px;' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
        esc(c.name || '—') +
      '</div>'
    );
    $band.append(
      '<div style="font-size:12px;opacity:0.65;margin-top:2px;' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
        esc(linkedName) +
      '</div>'
    );
    $band.append(
      '<div style="font-size:13px;opacity:0.7;margin-top:8px;letter-spacing:0.08em;">' +
        '•••• •••• •••• ' + esc(last4) +
      '</div>'
    );
    $band.append(
      '<div style="position:absolute;bottom:14px;right:16px;font-size:11px;opacity:0.55;">' +
        'Vence dia ' + esc(dueDay) +
      '</div>'
    );
    $card.append($band);

    // ── Fatura atual + Limite ──────────────────────────────
    let faturaValueHtml;
    if (loadingInv) {
      faturaValueHtml = '<span style="font-size:13px;color:var(--text-muted);">…</span>';
    } else {
      const faturaColor = used > 0 ? 'var(--expense)' : 'var(--text-primary)';
      faturaValueHtml =
        '<span style="font-size:20px;font-weight:800;color:' + faturaColor + ';">' +
          esc(fmt(used)) +
        '</span>';
    }

    const $body = $(
      '<div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;">' +
        '<div>' +
          '<p style="font-size:11px;color:var(--text-muted);font-weight:700;' +
            'text-transform:uppercase;letter-spacing:0.04em;">Fatura atual</p>' +
          '<p data-region="fatura-value" style="margin-top:4px;">' + faturaValueHtml + '</p>' +
        '</div>' +
        '<div style="text-align:right;">' +
          '<p style="font-size:11px;color:var(--text-muted);font-weight:700;' +
            'text-transform:uppercase;letter-spacing:0.04em;">Limite</p>' +
          '<p style="font-size:20px;font-weight:800;color:var(--text-primary);margin-top:4px;">' +
            esc(fmt(limit)) +
          '</p>' +
        '</div>' +
      '</div>'
    );
    $card.append($body);

    // ── Usage bar ──────────────────────────────────────────
    const $bar = $(
      '<div>' +
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

    // ── Footer: closing/due days + Ver fatura ──────────────
    const $footer = $(
      '<div style="display:flex;justify-content:space-between;align-items:center;' +
        'padding-top:12px;border-top:1px solid var(--border-light);">' +
        '<span style="font-size:12px;color:var(--text-muted);">' +
          'Fech. dia ' + esc(closingDay) + ' · Venc. dia ' + esc(dueDay) +
        '</span>' +
        '<button type="button" class="cc" data-act="view-invoice" data-id="' + esc(c.id) + '" ' +
          'style="background:transparent;border:none;cursor:pointer;font-size:12px;' +
          'font-weight:600;color:var(--accent);display:inline-flex;align-items:center;gap:4px;">' +
          'Ver fatura ' + window.icon('chevronRight', 12) +
        '</button>' +
      '</div>'
    );
    $card.append($footer);

    // Hover highlight border with card color
    $card.on('mouseenter', function () { $card.css('border-color', color); });
    $card.on('mouseleave', function () { $card.css('border-color', ''); });

    return $card;
  }

  // ── Invoice modal ─────────────────────────────────────────
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
              'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
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
    const $footer = $('<div style="display:flex;gap:10px;"></div>').append($close);

    const m = window.modal({
      title: 'Fatura · ' + (card.name || 'Cartão'),
      body: headerSummary + listHtml,
      footer: $footer[0].outerHTML,
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
    // Click anywhere on a card = view invoice.
    $root.on('click.cc', '.card.cc', function (e) {
      if ($(e.target).closest('[data-act]').length) return;
      const id = $(this).attr('data-id');
      if (id) openInvoiceModal(id);
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['credit-cards'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      syncCardsFromCache();
      render();
      loadAllTx();
      state.unsubscribe = window.App.AccountService.onChange(function () {
        syncCardsFromCache();
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

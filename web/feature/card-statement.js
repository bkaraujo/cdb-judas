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
 * pelo "Ver fatura" da tela de Cartões. Mesma fatia f003 de feature/credit-cards.js — lê
 * Domain.CreditCard e App.CreditCardService de lá.
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
    const $header = window.pageHeader({
      title: 'Extrato do Cartão',
      subtitle: cycleLabel || undefined,
      nav: window.periodNav({
        month: state.month,
        year: state.year,
        onPrev: function () { window.shiftMonth(state, -1, true); reloadPeriod(); },
        onNext: function () { window.shiftMonth(state, +1, true); reloadPeriod(); },
        onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m; state.year = y; reloadPeriod(); },
      })
    });

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
        $left.append(window.selectorButtonHtml({
          id: c.id, active: active,
          title: ((c.account && c.account.name) || '—') + ' •••• ' + c.last4,
          value: fmt(total), valueColor: total > 0 ? 'var(--expense)' : 'var(--text-muted)',
          cls: 'cst', act: 'select-card',
        }));
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

      const actionsHtml = isHeader ? '' : window.rowActionsHtml(tx.id);

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

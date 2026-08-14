/* feature/accounts.js — fatia Contas (contas, saldo, fechamento contábil — fundidos, mesma
 * fatia fina do backend f002). Um arquivo por fatia: application → infrastructure/secondary →
 * infrastructure/primary, cada bloco abaixo é um IIFE independente (comentário original de cada
 * arquivo preservado como separador de seção). Sem domain própria: Domain.Account é kernel
 * (core/kernel/_0_domain/account.js) — transactions/credit-cards/statement/dashboard precisam
 * da forma pura (currentBalance/hasCards). */

/* _2_application/account-service.js — Account use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()              { return repo.list(); }
  function create(data)        { return repo.create(data); }
  function update(id, data)    { return repo.update(id, data); }
  function remove(id, opts)    { return repo.remove(id, opts); }

  /* List from cache (no network) — used by views that need accounts in sync
     with SSE updates. */
  function listCached()        { return cache.accounts(); }

  function findById(id) { return cache.findById('accounts', id); }

  function addCard(accountId, data)      { return repo.createCard(accountId, data); }
  function removeCard(accountId, cardId, opts) { return repo.removeCard(accountId, cardId, opts); }
  function setCardActive(accountId, cardId, active) { return repo.setCardActive(accountId, cardId, active); }

  function onChange(cb) { return cache.subscribe('ACCOUNT', cb); }

  window.App = window.App || {};
  window.App.AccountService = {
    init: init,
    list: list,
    listCached: listCached,
    create: create,
    update: update,
    remove: remove,
    addCard: addCard,
    removeCard: removeCard,
    setCardActive: setCardActive,
    findById: findById,
    onChange: onChange,
  };
})();

/* _2_application/balance-service.js — monthly-balance reads.
 * Single owner of monthly-balance snapshot access for the frontend. */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  /* Resolves to the MonthlyBalance { id, accountId, period, balance } for the
     account/period, or rejects with err.status === 404 when no snapshot exists. */
  function monthly(accountId, period) {
    return repo.monthly(accountId, window.Domain.Period.yyyymm(period));
  }

  /* Resolves to BalanceResponse[] ({ accountId, period, balance }) for every account of the
     current user in the given period, in a single request — accounts without a snapshot for
     the period (e.g. before their first activity) are simply absent from the list. */
  function allAccounts(period) {
    return repo.allAccounts(window.Domain.Period.yyyymm(period));
  }

  window.App = window.App || {};
  window.App.BalanceService = {
    init: init,
    monthly: monthly,
    allAccounts: allAccounts,
  };
})();

/* _2_application/closing-service.js — accounting closing period. */
(function () {
  let repo = null;
  function init(deps) { repo = deps.repo; return { ready: true }; }

  function get()         { return repo.get(); }
  function set(period)   { return repo.set(period); }
  function clear()       { return repo.clear(); }

  window.App = window.App || {};
  window.App.ClosingService = {
    init: init, get: get, set: set, clear: clear,
  };
})();

/* _3_infrastructure/secondary/account-repository.js — HTTP adapter for /accounts (+ nested cards). */
(function () {
  function create(http) {
    return {
      list:    function ()        { return http.get('/accounts'); },
      create:  function (data)    { return http.post('/accounts', data); },
      update:  function (id, d)   { return http.patch('/accounts/' + id, d); },
      remove:  function (id, opts) { return http.delete('/accounts/' + id + window.Infra.HttpClient.deletionQuery(opts)); },
      listCards:  function (accountId)         { return http.get('/accounts/' + accountId + '/cards'); },
      createCard: function (accountId, data)   { return http.post('/accounts/' + accountId + '/cards', data); },
      removeCard: function (accountId, cardId, opts) {
        return http.delete('/accounts/' + accountId + '/cards/' + cardId + window.Infra.HttpClient.deletionQuery(opts));
      },
      setCardActive: function (accountId, cardId, active) {
        return http.patch('/accounts/' + accountId + '/cards/' + cardId, { active: active });
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.AccountRepository = { create: create };
})();

/* _3_infrastructure/secondary/balance-repository.js — HTTP adapter for the monthly
 * account-balance snapshot under the accounts namespace: GET /accounts/{id}/balance?period=yyyyMM
 * and the cross-account batch GET /accounts/balance?period=yyyyMM. */
(function () {
  function create(http) {
    return {
      monthly: function (id, yyyyMM) {
        return http.get('/accounts/' + id + '/balance?period=' + yyyyMM);
      },
      allAccounts: function (yyyyMM) {
        return http.get('/accounts/balance?period=' + yyyyMM);
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.BalanceRepository = { create: create };
})();

/* _3_infrastructure/secondary/closing-repository.js — HTTP adapter for /accounts/closing. */
(function () {
  function create(http) {
    return {
      get:   function ()       { return http.get('/accounts/closing'); },
      set:   function (period) { return http.post('/accounts/closing', { period: period }); },
      clear: function ()       { return http.delete('/accounts/closing'); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.ClosingRepository = { create: create };
})();

/* _3_infrastructure/primary/closing-dialog.js — set/clear the accounting closing period.
 *
 * window.closingDialog({ current, onChange })
 *   current: "YYYY-MM" string of the active closing, or null.
 *   onChange(newPeriodOrNull) — called after a successful save/clear so the caller
 *     (sidebar) can update its cached state and re-render.
 * Opens immediately and returns the modal handle, mirroring confirmModal/linkedDeleteDialog.
 * Load order: after ui.js/helpers.js/icons.js (see _3_infrastructure.js).
 */
(function () {
  function monthYearLabel(p) {
    const d = new Date(p.year, p.month - 1, 1);
    const name = new Intl.DateTimeFormat('pt-BR', { month: 'long' }).format(d);
    return name.charAt(0).toUpperCase() + name.slice(1) + '/' + p.year;
  }

  function closingDialog(opts) {
    opts = opts || {};
    const current = opts.current || null;
    let sel = window.Domain.Period.fromYyyyDashMm(current) || window.Domain.Period.currentMonth();

    const bodyHtml =
      window.modalText('Lançamentos e transferências até o mês escolhido ficam bloqueados para criação, edição e exclusão.') +
      '<div data-region="period-nav"></div>' +
      '<p style="font-size:12px;color:var(--text-secondary);margin-top:10px;">' +
        (current ? 'Fechamento atual: ' + window.esc(monthYearLabel(window.Domain.Period.fromYyyyDashMm(current)))
                 : 'Nenhum fechamento definido') +
      '</p>';

    const $clear = window.btn({
      variant: 'danger', size: 'md', label: 'Limpar', disabled: !current,
      attrs: 'data-act="closing-clear" type="button"',
    });
    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"',
    });
    const $save = window.btn({
      variant: 'primary', size: 'md', label: 'Salvar',
      attrs: 'data-act="closing-save" type="button"',
    });

    const m = window.modal({
      title: 'Fechamento contábil',
      body: bodyHtml,
      footer: window.modalFooter([$clear, $cancel, $save], { align: 'end' }),
    });
    m.open();

    function renderNav() {
      const $region = m.$body.find('[data-region=period-nav]');
      $region.empty().append(window.periodNav({
        month: sel.month,
        year: sel.year,
        onPrev:   function () { sel = window.Domain.Period.shift(sel, -1); renderNav(); },
        onNext:   function () { sel = window.Domain.Period.shift(sel, 1); renderNav(); },
        onChange: function (mo, yr) { sel = window.Domain.Period.create(mo, yr); renderNav(); },
      }));
    }
    renderNav();

    m.$el.on('click', '[data-act=closing-save]', function () {
      const $b = $(this);
      const period = window.Domain.Period.yyyyDashMm(sel);
      window.runMutation(window.App.ClosingService.set(period), {
        $btn: $b,
        modal: m,
        success: 'Fechamento definido',
        failure: 'Falha ao salvar fechamento',
        onDone: function () { if (opts.onChange) opts.onChange(period); },
      });
    });

    m.$el.on('click', '[data-act=closing-clear]', function () {
      const $b = $(this);
      window.runMutation(window.App.ClosingService.clear(), {
        $btn: $b,
        modal: m,
        success: 'Fechamento removido',
        failure: 'Falha ao salvar fechamento',
        onDone: function () { if (opts.onChange) opts.onChange(null); },
      });
    });

    return m;
  }

  window.closingDialog = closingDialog;
})();

/* pages/accounts.js — Contas Bancárias (CRUD em grid de cards).
 *
 * Cartões de crédito não são mais um tipo de conta: são entidades filhas (last4
 * apenas), geridas dentro do modal de edição da conta real a que pertencem. O
 * limite de crédito/cheque especial e o ciclo de fatura (fechamento/vencimento)
 * são configuração da conta (seção "Limites e Fatura", sempre visível).
 */
(function () {
  window.Pages = window.Pages || {};

  const TYPE_LABELS = {
    CHECKING: 'Conta Corrente',
    INVESTMENT: 'Investimento',
  };

  // ── State ─────────────────────────────────────────────────
  let state = null;

  function resetState() {
    state = {
      accounts: [],
      $root: null,
    };
    return state;
  }

  // ── Helpers ───────────────────────────────────────────────

  function findAccount(id) { return window.byId(state.accounts, id); }

  function colorOf(a) { return a.color || '#6366F1'; }

  function iconForType(type) {
    return type === 'INVESTMENT' ? 'trendingUp' : 'building';
  }

  // Optional numeric fields (Limites e Fatura): blank input → null payload.
  function parseOptionalCurrency(raw) {
    const n = window.parseCurrency(raw);
    return isFinite(n) ? n.toFixed(2) : null;
  }

  function parseOptionalDay(raw) {
    const n = parseInt(raw, 10);
    return (isFinite(n) && n >= 1 && n <= 31) ? n : null;
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    // Page header.
    const $header = window.pageHeader({
      title: 'Contas Bancárias',
      actions: window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Conta',
        attrs: 'data-act="new"'
      })
    });

    let $body;
    if (!state.accounts.length) {
      $body = $(window.emptyState({
        icon: 'building',
        title: 'Nenhuma conta cadastrada',
        desc: 'Clique em "Nova Conta" para começar.'
      }));
    } else {
      $body = $(
        '<div data-region="accounts-grid" style="' +
          'display:grid;gap:14px;' +
          'grid-template-columns:repeat(auto-fill, minmax(280px, 1fr));' +
        '"></div>'
      );
      state.accounts.slice().sort(window.sortByName).forEach(function (a) { $body.append(renderCard(a)); });
    }

    $root.empty().append($header).append($body);
  }

  function renderCard(a) {
    const color = colorOf(a);
    const typeLabel = TYPE_LABELS[a.type] || 'Conta';
    const iconName = iconForType(a.type);

    const displayValue = window.Domain.Account.currentBalance(a);
    const valueColor = displayValue >= 0 ? 'var(--income)' : 'var(--expense)';
    const active = a.active !== false;

    const $card = $(
      '<div class="card" data-card="account" data-id="' + esc(a.id) + '" style="' +
        'padding:0;overflow:hidden;position:relative;display:flex;flex-direction:column;' +
        'transition:transform var(--transition), border-color var(--transition);' +
      '"></div>'
    );

    // Colored top band.
    $card.append(
      '<div style="height:4px;background:' + esc(color) + ';"></div>'
    );

    // Header: dot + name/type ; action buttons top-right.
    const $head = $(
      '<div style="' +
        'display:flex;align-items:flex-start;justify-content:space-between;' +
        'gap:10px;padding:16px 16px 8px 16px;' +
      '"></div>'
    );

    const $left = $(
      '<div style="display:flex;align-items:center;gap:12px;min-width:0;flex:1;"></div>'
    );
    $left.append(
      '<div style="' +
        'width:40px;height:40px;border-radius:10px;flex-shrink:0;' +
        'background:' + esc(color) + '26;color:' + esc(color) + ';' +
        'display:flex;align-items:center;justify-content:center;' +
      '">' + window.icon(iconName, 20) + '</div>'
    );

    $left.append(
      '<div style="min-width:0;display:flex;flex-direction:column;gap:2px;">' +
        '<div style="font-size:14px;font-weight:700;color:var(--text-primary);' +
          'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + esc(a.name) + '</div>' +
        '<div style="font-size:12px;color:var(--text-muted);font-weight:500;">' + esc(typeLabel) + '</div>' +
      '</div>'
    );
    $head.append($left);

    const $actions = $(
      '<div style="display:flex;gap:2px;flex-shrink:0;"></div>'
    );
    $actions.append(window.rowActionBtn('edit',  'Editar',  a.id));
    $actions.append(window.rowActionBtn('trash', 'Excluir', a.id, true));
    $head.append($actions);

    $card.append($head);

    // Value block.
    $card.append(
      '<div style="padding:6px 16px 16px 16px;">' +
        '<div style="font-size:11px;color:var(--text-muted);font-weight:600;' +
          'text-transform:uppercase;letter-spacing:0.04em;">Saldo atual</div>' +
        '<div style="font-size:20px;font-weight:800;color:' + valueColor + ';margin-top:2px;">' +
          esc(fmt(displayValue)) +
        '</div>' +
      '</div>'
    );

    // Cards summary (only when the account has attached cards).
    if (window.Domain.Account.hasCards(a)) {
      const last4Line = a.cards.map(function (c) { return '•••• ' + c.last4; }).join('  ·  ');
      $card.append(
        '<div style="display:flex;align-items:center;gap:6px;padding:0 16px 14px 16px;' +
          'font-size:12px;color:var(--text-muted);">' +
          window.icon('creditCard', 13) +
          '<span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + esc(last4Line) + '</span>' +
        '</div>'
      );
    }

    // Footer with active status (subtle).
    if (!active) {
      $card.append(
        '<div style="' +
          'padding:8px 16px;border-top:1px solid var(--border-light);' +
          'font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.04em;' +
          'color:var(--text-muted);' +
        '">Inativa</div>'
      );
    }

    return $card;
  }

  // ── Modal: create / edit ──────────────────────────────────
  function openFormModal(existing) {
    const isEdit = !!existing;
    const uniq = Date.now();
    const ids = {
      name: 'acc-name-' + uniq,
      type: 'acc-type-' + uniq,
      color: 'acc-color-' + uniq,
      active: 'acc-active-' + uniq,
      limit: 'acc-limit-' + uniq,
      overdraft: 'acc-overdraft-' + uniq,
      closingDay: 'acc-closing-' + uniq,
      dueDay: 'acc-due-' + uniq,
      cardLast4: 'acc-card-last4-' + uniq,
    };

    const initial = {
      name: isEdit ? (existing.name || '') : '',
      type: isEdit ? (existing.type || 'CHECKING') : 'CHECKING',
      color: isEdit ? (existing.color || window.PALETTE.swatches[0]) : window.PALETTE.swatches[0],
      active: isEdit ? (existing.active !== false) : true,
      creditLimit: isEdit && existing.creditLimit ? window.maskCurrency(existing.creditLimit) : '',
      overdraftLimit: isEdit && existing.overdraftLimit ? window.maskCurrency(existing.overdraftLimit) : '',
      closingDay: isEdit && existing.closingDay ? String(existing.closingDay) : '',
      dueDay: isEdit && existing.dueDay ? String(existing.dueDay) : '',
    };

    // Local, optimistic copy of this account's cards — kept in sync with the
    // server on every add/remove so the chip list reflects reality without
    // waiting for the SSE round-trip.
    let cards = isEdit ? (existing.cards || []).slice() : [];

    const typeOptions = [
      ['CHECKING', 'Corrente'],
      ['INVESTMENT', 'Investimento'],
    ].map(function (p) {
      const sel = p[0] === initial.type ? ' selected' : '';
      return '<option value="' + esc(p[0]) + '"' + sel + '>' + esc(p[1]) + '</option>';
    }).join('');

    function cardsRegionHtml() {
      if (!isEdit) {
        return '<p style="font-size:12px;color:var(--text-muted);">' +
          'Salve a conta antes de adicionar cartões.' +
        '</p>';
      }
      const chips = cards.map(function (c) {
        const active = c.active !== false;
        const actionHtml = active
          ? '<button type="button" data-act="remove-card" data-id="' + esc(c.id) + '" ' +
              'style="background:none;border:none;cursor:pointer;color:var(--text-muted);' +
              'padding:0;display:inline-flex;" title="Remover cartão">' + window.icon('x', 12) + '</button>'
          : '<button type="button" data-act="reactivate-card" data-id="' + esc(c.id) + '" ' +
              'style="background:none;border:none;cursor:pointer;color:var(--text-muted);' +
              'padding:0;display:inline-flex;" title="Reativar cartão">' + window.icon('eye', 12) + '</button>';
        return '<span class="chip" data-card-id="' + esc(c.id) + '" style="' +
            (active ? '' : 'opacity:0.55;') + '">' +
          '<span>•••• ' + esc(c.last4) + (active ? '' : ' · Inativo') + '</span>' +
          '<span class="chip-actions">' + actionHtml + '</span>' +
        '</span>';
      }).join('');
      const chipsHtml = chips
        ? '<div class="chip-group" style="margin-bottom:10px;">' + chips + '</div>'
        : '<p style="font-size:12px;color:var(--text-muted);margin-bottom:10px;">Nenhum cartão nesta conta.</p>';
      return chipsHtml +
        '<div style="display:flex;gap:8px;align-items:center;">' +
          '<input id="' + ids.cardLast4 + '" data-field="new-card-last4" type="text" maxlength="4" ' +
            'inputmode="numeric" placeholder="0000" style="width:90px;" />' +
          '<button type="button" data-act="add-card" class="btn btn-secondary btn-sm">' +
            window.icon('plus', 13) + '<span>Adicionar</span>' +
          '</button>' +
        '</div>';
    }

    const bodyHtml =
      '<form data-form="acc" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            window.colorNameFieldHtml({
              colorId: ids.color, nameId: ids.name, color: initial.color, nameValue: initial.name,
              placeholder: 'Ex: Itaú, Nubank...', swatchMarginTop: '6px',
            }) +
          '</div>' +

          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.type + '">Tipo</label>' +
            '<select id="' + ids.type + '" name="type">' + typeOptions + '</select>' +
          '</div>' +
          '<div class="form-group full">' +
            '<label style="display:inline-flex;align-items:center;gap:8px;cursor:pointer;' +
              'font-size:13px;font-weight:500;color:var(--text-primary);">' +
              '<input id="' + ids.active + '" name="active" type="checkbox"' +
                (initial.active ? ' checked' : '') +
                ' style="width:16px;height:16px;accent-color:var(--accent);" />' +
              'Conta ativa' +
            '</label>' +
          '</div>' +
        '</div>' +

        // Limites e Fatura — sempre visível (qualquer conta pode hospedar cartões).
        '<div style="margin-top:14px;padding-top:14px;border-top:1px solid var(--border);">' +
          '<p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:10px;">' +
            'Limites e Fatura' +
          '</p>' +
          '<div class="form-grid">' +
            '<div class="form-group">' +
              '<label class="form-label" for="' + ids.limit + '">Limite de crédito</label>' +
              '<input id="' + ids.limit + '" name="creditLimit" type="text" inputmode="numeric" ' +
                'placeholder="0,00" value="' + esc(initial.creditLimit) + '" />' +
            '</div>' +
            '<div class="form-group">' +
              '<label class="form-label" for="' + ids.overdraft + '">Cheque especial</label>' +
              '<input id="' + ids.overdraft + '" name="overdraftLimit" type="text" inputmode="numeric" ' +
                'placeholder="0,00" value="' + esc(initial.overdraftLimit) + '" />' +
            '</div>' +
            '<div class="form-group">' +
              '<label class="form-label" for="' + ids.closingDay + '">Fechamento (dia)</label>' +
              '<input id="' + ids.closingDay + '" name="closingDay" type="number" min="1" max="31" ' +
                'value="' + esc(initial.closingDay) + '" />' +
            '</div>' +
            '<div class="form-group">' +
              '<label class="form-label" for="' + ids.dueDay + '">Vencimento (dia)</label>' +
              '<input id="' + ids.dueDay + '" name="dueDay" type="number" min="1" max="31" ' +
                'value="' + esc(initial.dueDay) + '" />' +
            '</div>' +
          '</div>' +
        '</div>' +

        // Cartões — apenas em edição (precisa de um accountId salvo).
        '<div data-region="cards-section" style="margin-top:14px;padding-top:14px;border-top:1px solid var(--border);">' +
          '<p style="font-size:12px;font-weight:700;color:var(--text-secondary);margin-bottom:10px;">Cartões</p>' +
          '<div data-region="cards-body">' + cardsRegionHtml() + '</div>' +
        '</div>' +
      '</form>';

    const m = window.formModal({
      title: isEdit ? 'Editar Conta' : 'Nova Conta',
      formName: 'acc',
      body: bodyHtml,
      onSubmit: function ($form) {
        const name = ($form.find('input[name=name]').val() || '').trim();
        if (!name) {
          $form.find('input[name=name]').trigger('focus');
          return null;
        }
        const payload = {
          name: name,
          type: $form.find('select[name=type]').val(),
          color: $form.find('input[name=color]').val() || window.PALETTE.swatches[0],
          active: $form.find('input[name=active]').is(':checked'),
          creditLimit: parseOptionalCurrency($form.find('input[name=creditLimit]').val()),
          overdraftLimit: parseOptionalCurrency($form.find('input[name=overdraftLimit]').val()),
          closingDay: parseOptionalDay($form.find('input[name=closingDay]').val()),
          dueDay: parseOptionalDay($form.find('input[name=dueDay]').val()),
        };
        return isEdit
          ? window.App.AccountService.update(existing.id, payload)
          : window.App.AccountService.create(payload);
      },
      success: function () { return isEdit ? 'Conta atualizada' : 'Conta criada'; },
      failure: 'Falha ao salvar conta',
      // SSE UPSERT will refresh CacheStore → AccountService.onChange → re-render.
    });

    const $form = m.$body.find('form[data-form=acc]');
    const $color = $form.find('input[name=color]');
    const $limit = $form.find('input[name=creditLimit]');
    const $overdraft = $form.find('input[name=overdraftLimit]');

    window.bindSwatches(m, $color);

    // Currency masks.
    window.bindCurrencyMask($limit);
    window.bindCurrencyMask($overdraft);

    // ── Cartões: add/remove live (endpoints próprios, fora do submit do form) ─
    function renderCardsBody() {
      m.$body.find('[data-region=cards-body]').html(cardsRegionHtml());
    }

    m.$el.on('click', '[data-act=add-card]', function () {
      if (!isEdit) return;
      const $input = m.$body.find('#' + ids.cardLast4);
      const last4 = ($input.val() || '').trim();
      if (!/^\d{4}$/.test(last4)) {
        window.toast('Informe os 4 dígitos do cartão', 'error');
        return;
      }
      const $btn = $(this);
      window.runMutation(window.App.AccountService.addCard(existing.id, { last4: last4 }), {
        $btn: $btn,
        success: 'Cartão adicionado',
        failure: 'Falha ao adicionar cartão',
        onDone: function (card) { cards.push(card); renderCardsBody(); },
      });
    });

    function doRemoveCard(cardId, opts, dialogModal, dialogReEnable) {
      const $chip = m.$body.find('[data-card-id="' + cardId + '"]');
      $chip.css('opacity', '0.5');
      window.runMutation(window.App.AccountService.removeCard(existing.id, cardId, opts), {
        modal: dialogModal,
        success: 'Cartão removido',
        failure: 'Falha ao remover cartão',
        onDone: function () {
          cards = cards.filter(function (c) { return String(c.id) !== String(cardId); });
          renderCardsBody();
        },
        onError: function (err) {
          $chip.css('opacity', '1');
          if (!opts && err && err.status === 409 && err.code === 'LINKED_TRANSACTIONS') {
            openLinkedCardDialog(cardId, err.count);
            return true;
          }
          if (dialogReEnable) dialogReEnable();
        },
      });
    }

    function openLinkedCardDialog(cardId, count) {
      const card = cards.filter(function (c) { return String(c.id) === String(cardId); })[0];
      if (!card) return;
      const otherActive = cards.filter(function (c) {
        return String(c.id) !== String(cardId) && c.active !== false;
      });
      const options = [
        {
          value: 'MOVE', label: 'Mover para outro cartão',
          hint: 'As transações deste cartão passam para o cartão escolhido (mesma conta).',
          choices: otherActive.map(function (c) { return { value: c.id, label: '•••• ' + c.last4 }; }),
        },
        {
          value: 'DELETE', label: 'Excluir transações', danger: true,
          hint: 'Apaga o cartão e ' + window.pluralTransactions(count) + '.',
        },
        { value: 'INATIVAR', label: 'Inativar cartão', hint: 'Some de novos lançamentos; o histórico é mantido.' },
      ];

      window.linkedDeleteDialog({
        title: 'Cartão com transações vinculadas',
        intro: 'O cartão <strong>•••• ' + esc(card.last4) + '</strong> tem ' + window.pluralTransactions(count) + '. Escolha o que fazer:',
        options: options,
        onConfirm: function (choice, m2, reEnable) {
          if (choice.strategy === 'INATIVAR') {
            window.runMutation(window.App.AccountService.setCardActive(existing.id, cardId, false), {
              modal: m2,
              success: 'Cartão inativado',
              failure: 'Falha ao inativar cartão',
              onDone: function (updated) {
                cards = cards.map(function (c) { return String(c.id) === String(updated.id) ? updated : c; });
                renderCardsBody();
              },
              onError: function () { reEnable(); },
            });
            return;
          }
          doRemoveCard(cardId, { strategy: choice.strategy, targetId: choice.targetId }, m2, reEnable);
        },
      });
    }

    m.$el.on('click', '[data-act=remove-card]', function () {
      if (!isEdit) return;
      const cardId = $(this).attr('data-id');
      const card = cards.filter(function (c) { return String(c.id) === String(cardId); })[0];
      if (!card) return;
      doRemoveCard(cardId);
    });

    m.$el.on('click', '[data-act=reactivate-card]', function () {
      if (!isEdit) return;
      const cardId = $(this).attr('data-id');
      window.runMutation(window.App.AccountService.setCardActive(existing.id, cardId, true), {
        success: 'Cartão reativado',
        failure: 'Falha ao reativar cartão',
        onDone: function (updated) {
          cards = cards.map(function (c) { return String(c.id) === String(updated.id) ? updated : c; });
          renderCardsBody();
        },
      });
    });

    m.$body.on('keydown', '[data-field=new-card-last4]', function (e) {
      if (e.key === 'Enter') {
        e.preventDefault();
        m.$el.find('[data-act=add-card]').trigger('click');
      }
    });

  }

  // ── Modal: confirm delete ─────────────────────────────────
  function openDeleteModal(target) {
    window.deleteWithLinkedFallback({
      title: 'Excluir Conta',
      body: '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
        'Tem certeza que deseja excluir a conta <strong>' + esc(target.name) + '</strong>? ' +
        'Esta ação não pode ser desfeita.</p>',
      remove: function () { return window.App.AccountService.remove(target.id); },
      success: 'Conta excluída',
      failure: 'Falha ao excluir conta',
      // SSE DELETE will refresh CacheStore → AccountService.onChange → re-render.
      linked: {
        title: 'Conta com transações vinculadas',
        intro: function (count) {
          return 'A conta <strong>' + esc(target.name) + '</strong> tem ' + window.pluralTransactions(count) + '. Escolha o que fazer:';
        },
        options: function (count) {
          const otherActive = state.accounts.filter(function (a) {
            return String(a.id) !== String(target.id) && a.active !== false;
          });
          return [
            {
              value: 'MOVE', label: 'Mover para outra conta',
              hint: 'Transações e cartões desta conta passam para a conta escolhida.',
              choices: otherActive.map(function (a) { return { value: a.id, label: a.name }; }),
            },
            {
              value: 'DELETE', label: 'Excluir transações', danger: true,
              hint: 'Apaga a conta e ' + window.pluralTransactions(count) + '.',
            },
            { value: 'INATIVAR', label: 'Inativar conta', hint: 'Some do lançamento de novas transações; o histórico é mantido.' },
          ];
        },
        dispatch: function (choice) {
          if (choice.strategy === 'INATIVAR') {
            return window.App.AccountService.update(target.id, {
              name: target.name, type: target.type, color: target.color, active: false,
              creditLimit: target.creditLimit || null,
              overdraftLimit: target.overdraftLimit || null,
              closingDay: target.closingDay || null,
              dueDay: target.dueDay || null,
            });
          }
          return window.App.AccountService.remove(target.id, { strategy: choice.strategy, targetId: choice.targetId });
        },
        success: function (choice) { return choice.strategy === 'INATIVAR' ? 'Conta inativada' : 'Conta excluída'; },
        failure: 'Falha ao excluir conta',
      },
    });
  }

  // ── Event delegation on $root ─────────────────────────────
  function bindRoot($root) {
    $root.on('click.accs', '[data-act=new]', function () {
      openFormModal(null);
    });
    $root.on('click.accs', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const acc = findAccount(id);
      if (acc) openFormModal(acc);
    });
    $root.on('click.accs', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const acc = findAccount(id);
      if (acc) openDeleteModal(acc);
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['accounts'] = window.cachePage({
    ns: '.accs',
    collection: 'accounts',
    event: 'ACCOUNT',
    state: resetState,
    render: render,
    bind: bindRoot,
  });
})();

/* pages/accounts.js — Contas Bancárias (CRUD em grid de cards). */
(function () {
  window.Pages = window.Pages || {};

  const TYPE_LABELS = {
    CHECKING: 'Conta Corrente',
    SAVINGS: 'Conta Poupança',
    INVESTMENT: 'Investimento',
    CREDIT_CARD: 'Cartão de Crédito'
  };

  const DEFAULT_COLORS = [
    '#6366F1', '#10B981', '#F43F5E', '#F59E0B',
    '#38BDF8', '#A78BFA', '#820AD1', '#1C2951'
  ];

  // ── State ─────────────────────────────────────────────────
  let state = null;

  function resetState() {
    state = {
      accounts: [],
      $root: null,
    };
  }

  // Accounts live in the App.CacheStore (hydrated at login, SSE-refreshed).
  function syncFromCache() {
    state.accounts = window.App.CacheStore.accounts().slice();
  }

  // ── Toast (same pattern as categories.js) ─────────────────

  // ── Helpers ───────────────────────────────────────────────

  function findAccount(id) {
    for (let i = 0; i < state.accounts.length; i++) {
      if (String(state.accounts[i].id) === String(id)) return state.accounts[i];
    }
    return null;
  }

  function colorOf(a) { return a.color || '#6366F1'; }

  function iconForType(type) {
    return type === 'CREDIT_CARD' ? 'creditCard' : 'building';
  }

  // BR currency mask for the modal input.
  function maskCurrency(input) {
    let digits = String(input == null ? '' : input).replace(/\D/g, '');
    if (!digits) return '0,00';
    digits = digits.replace(/^0+/, '') || '0';
    while (digits.length < 3) digits = '0' + digits;
    const cents = digits.slice(-2);
    const whole = digits.slice(0, -2);
    const withSep = whole.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    return withSep + ',' + cents;
  }

  function parseCurrency(masked) {
    const s = String(masked == null ? '' : masked).replace(/\./g, '').replace(',', '.');
    const n = parseFloat(s);
    return isNaN(n) ? 0 : n;
  }

  function maskInitial(n) {
    return maskCurrency(String(Math.round((Number(n) || 0) * 100)));
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    // Page header.
    const $header = $(
      '<div class="page-header">' +
        '<h1>Contas Bancárias</h1>' +
        '<div class="page-header-actions" data-region="actions"></div>' +
      '</div>'
    );
    $header.find('[data-region=actions]').append(
      window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Conta',
        attrs: 'data-act="new"'
      })
    );

    let $body;
    if (!state.accounts.length) {
      $body = $(window.emptyState({
        icon: 'building',
        title: 'Nenhuma conta cadastrada',
        desc: 'Clique em "Nova Conta" para começar.'
      }));
    } else {
      $body = $('<div></div>');
      
      const accounts = state.accounts.filter(function(a) { return a.type !== 'CREDIT_CARD'; }).sort(window.sortByName);
      const creditCards = state.accounts.filter(function(a) { return a.type === 'CREDIT_CARD'; }).sort(window.sortByName);

      if (accounts.length > 0) {
        $body.append('<h2 style="font-size:16px;font-weight:600;margin:0 0 12px 0;color:var(--text-primary);">Contas</h2>');
        const $accGrid = $(
          '<div data-region="accounts-grid" style="' +
            'display:grid;gap:14px;' +
            'grid-template-columns:repeat(auto-fill, minmax(280px, 1fr));' +
            'margin-bottom:24px;' +
          '"></div>'
        );
        accounts.forEach(function (a) { $accGrid.append(renderCard(a)); });
        $body.append($accGrid);
      }

      if (creditCards.length > 0) {
        $body.append('<h2 style="font-size:16px;font-weight:600;margin:0 0 12px 0;color:var(--text-primary);">Cartões de Crédito</h2>');
        const $ccGrid = $(
          '<div data-region="credit-cards-grid" style="' +
            'display:grid;gap:14px;' +
            'grid-template-columns:repeat(auto-fill, minmax(280px, 1fr));' +
          '"></div>'
        );
        creditCards.forEach(function (a) { $ccGrid.append(renderCard(a)); });
        $body.append($ccGrid);
      }
    }

    $root.empty().append($header).append($body);
  }

  function renderCard(a) {
    const color = colorOf(a);
    const typeLabel = TYPE_LABELS[a.type] || 'Conta';
    const iconName = iconForType(a.type);

    // Credit cards show available limit; other accounts show current balance.
    const displayValue = window.Domain.Account.displayBalance(a);
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
    let subTitle = esc(typeLabel);
    if (a.type === 'CREDIT_CARD' && a.linkedAccountId) {
      const linked = findAccount(a.linkedAccountId);
      if (linked) {
        subTitle = esc(linked.name);
      }
    }

    $left.append(
      '<div style="min-width:0;display:flex;flex-direction:column;gap:2px;">' +
        '<div style="font-size:14px;font-weight:700;color:var(--text-primary);' +
          'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + esc(a.name) + '</div>' +
        '<div style="font-size:12px;color:var(--text-muted);font-weight:500;">' + subTitle + '</div>' +
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
    const valueLabel = a.type === 'CREDIT_CARD' ? 'Limite disponível' : 'Saldo atual';
    $card.append(
      '<div style="padding:6px 16px 16px 16px;">' +
        '<div style="font-size:11px;color:var(--text-muted);font-weight:600;' +
          'text-transform:uppercase;letter-spacing:0.04em;">' + esc(valueLabel) + '</div>' +
        '<div style="font-size:20px;font-weight:800;color:' + valueColor + ';margin-top:2px;">' +
          esc(fmt(displayValue)) +
        '</div>' +
      '</div>'
    );

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
      balance: 'acc-balance-' + uniq,
      active: 'acc-active-' + uniq,
      linked: 'acc-linked-' + uniq,
      last4: 'acc-last4-' + uniq,
      limit: 'acc-limit-' + uniq,
      closingDay: 'acc-closing-' + uniq,
      dueDay: 'acc-due-' + uniq,
    };

    const initial = {
      name: isEdit ? (existing.name || '') : '',
      type: isEdit ? (existing.type || 'CHECKING') : 'CHECKING',
      color: isEdit ? (existing.color || '#6366F1') : '#6366F1',
      balance: isEdit ? maskInitial(existing.balance) : '0,00',
      active: isEdit ? (existing.active !== false) : true,
      linkedAccountId: isEdit ? (existing.linkedAccountId ? String(existing.linkedAccountId) : '') : '',
      last4: isEdit && existing.additionalInfo ? (existing.additionalInfo.last4 || '') : '',
      limit: isEdit && existing.additionalInfo && existing.additionalInfo.limit != null
        ? maskInitial(existing.additionalInfo.limit) : '0,00',
      closingDay: isEdit && existing.additionalInfo && existing.additionalInfo.closingDay
        ? String(existing.additionalInfo.closingDay) : '1',
      dueDay: isEdit && existing.additionalInfo && existing.additionalInfo.dueDay
        ? String(existing.additionalInfo.dueDay) : '10',
    };

    const typeOptions = [
      ['CHECKING', 'Corrente'],
      ['SAVINGS', 'Poupança'],
      ['INVESTMENT', 'Investimento'],
      ['CREDIT_CARD', 'Cartão de Crédito']
    ].map(function (p) {
      const sel = p[0] === initial.type ? ' selected' : '';
      return '<option value="' + esc(p[0]) + '"' + sel + '>' + esc(p[1]) + '</option>';
    }).join('');

    // Linked-account options: CHECKING accounts (excluding self).
    const linkedOptions = '<option value="">Selecione uma conta...</option>' +
      window.App.AccountService.linkableCheckings(isEdit ? existing.id : null)
        .slice().sort(window.sortByName)
        .map(function (a) {
          const sel = String(a.id) === initial.linkedAccountId ? ' selected' : '';
          return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
        }).join('');

    const swatchesHtml = DEFAULT_COLORS.map(function (c) {
      const isSel = c.toLowerCase() === String(initial.color).toLowerCase();
      return '<button type="button" data-swatch="' + esc(c) + '" ' +
        'style="width:22px;height:22px;border-radius:50%;cursor:pointer;' +
        'border:' + (isSel ? '2px solid var(--text-primary)' : '1px solid var(--border)') + ';' +
        'background:' + esc(c) + ';padding:0;flex-shrink:0;" title="' + esc(c) + '"></button>';
    }).join('');

    const bodyHtml =
      '<form data-form="acc" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.name + '">Nome</label>' +
            '<div style="display:flex;align-items:center;gap:10px;">' +
              '<input id="' + ids.color + '" name="color" type="color" ' +
                'value="' + esc(initial.color) + '" ' +
                'style="width:40px;height:40px;border:1px solid var(--border);' +
                'border-radius:50%;padding:0;background:transparent;cursor:pointer;flex-shrink:0;" />' +
              '<input id="' + ids.name + '" name="name" type="text" required ' +
                'placeholder="Ex: Itaú, Nubank..." value="' + esc(initial.name) + '" />' +
            '</div>' +
            '<div data-region="swatches" style="display:flex;gap:6px;margin-top:6px;flex-wrap:wrap;">' +
              swatchesHtml +
            '</div>' +
          '</div>' +

          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.type + '">Tipo</label>' +
            '<select id="' + ids.type + '" name="type">' + typeOptions + '</select>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.balance + '">Saldo Inicial</label>' +
            '<input id="' + ids.balance + '" name="balance" type="text" inputmode="numeric" ' +
              'value="' + esc(initial.balance) + '" />' +
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

        // Credit-card extra fields (toggled).
        '<div data-region="cc-fields" style="' +
          'display:' + (initial.type === 'CREDIT_CARD' ? 'block' : 'none') + ';' +
          'margin-top:14px;padding-top:14px;border-top:1px solid var(--border);">' +
          '<div class="form-grid">' +
            '<div class="form-group full">' +
              '<label class="form-label" for="' + ids.linked + '">Conta Vinculada (Pagamento)</label>' +
              '<select id="' + ids.linked + '" name="linkedAccountId">' + linkedOptions + '</select>' +
            '</div>' +
            '<div class="form-group">' +
              '<label class="form-label" for="' + ids.last4 + '">Últimos 4 dígitos</label>' +
              '<input id="' + ids.last4 + '" name="last4" type="text" maxlength="4" ' +
                'placeholder="0000" value="' + esc(initial.last4) + '" />' +
            '</div>' +
            '<div class="form-group">' +
              '<label class="form-label" for="' + ids.limit + '">Limite</label>' +
              '<input id="' + ids.limit + '" name="limit" type="text" inputmode="numeric" ' +
                'value="' + esc(initial.limit) + '" />' +
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
      title: isEdit ? 'Editar Conta' : 'Nova Conta',
      body: bodyHtml,
      footer: $footer[0].outerHTML,
    });
    m.open();

    const $form = m.$body.find('form[data-form=acc]');
    const $type = $form.find('select[name=type]');
    const $color = $form.find('input[name=color]');
    const $cc = m.$body.find('[data-region=cc-fields]');
    const $balance = $form.find('input[name=balance]');
    const $limit = $form.find('input[name=limit]');

    // Type toggles credit-card fields and disables color picker.
    function syncTypeUI() {
      const t = $type.val();
      $cc.css('display', t === 'CREDIT_CARD' ? 'block' : 'none');
      $color.prop('disabled', t === 'CREDIT_CARD');
      $color.css('opacity', t === 'CREDIT_CARD' ? '0.5' : '1');
      m.$body.find('[data-region=swatches]').css('opacity', t === 'CREDIT_CARD' ? '0.5' : '1').css('pointer-events', t === 'CREDIT_CARD' ? 'none' : 'auto');
      if (t === 'CREDIT_CARD') {
        // Auto-pick color from linked account if available.
        const linkedId = $form.find('select[name=linkedAccountId]').val();
        if (linkedId) {
          const linked = findAccount(linkedId);
          if (linked && linked.color) {
            $color.val(linked.color);
            paintSwatches(linked.color);
          }
        }
      }
    }
    $type.on('change', syncTypeUI);
    syncTypeUI();
    $form.find('select[name=linkedAccountId]').on('change', function () {
      if ($type.val() === 'CREDIT_CARD') {
        const linked = findAccount($(this).val());
        if (linked && linked.color) {
          $color.val(linked.color);
          paintSwatches(linked.color);
        }
      }
    });

    // Swatch click → set color.
    function paintSwatches(activeHex) {
      m.$body.find('[data-region=swatches] [data-swatch]').each(function () {
        const c = $(this).attr('data-swatch');
        const on = c && c.toLowerCase() === String(activeHex).toLowerCase();
        $(this).css('border', on ? '2px solid var(--text-primary)' : '1px solid var(--border)');
      });
    }
    m.$body.on('click', '[data-swatch]', function (e) {
      e.preventDefault();
      if ($color.prop('disabled')) return;
      const c = $(this).attr('data-swatch');
      $color.val(c);
      paintSwatches(c);
    });
    $color.on('input change', function () { paintSwatches($color.val()); });

    // Currency masks.
    $balance.on('input', function () { $balance.val(maskCurrency($balance.val())); });
    $limit.on('input', function () { $limit.val(maskCurrency($limit.val())); });

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($form.find('input[name=name]').val() || '').trim();
      if (!name) {
        $form.find('input[name=name]').trigger('focus');
        return;
      }
      const t = $type.val();
      const payload = {
        name: name,
        balance: parseCurrency($balance.val()).toFixed(2),
        type: t,
        color: $color.val() || '#6366F1',
        active: $form.find('input[name=active]').is(':checked'),
      };
      if (t === 'CREDIT_CARD') {
        const linkedId = $form.find('select[name=linkedAccountId]').val();
        if (linkedId) payload.linkedAccountId = linkedId;
        payload.additionalInfo = {
          last4: ($form.find('input[name=last4]').val() || '').trim(),
          limit: parseCurrency($limit.val()).toFixed(2),
          closingDay: parseInt($form.find('input[name=closingDay]').val(), 10) || 1,
          dueDay: parseInt($form.find('input[name=dueDay]').val(), 10) || 10,
        };
      }

      const $btn = m.$el.find('[data-act=save]');
      $btn.prop('disabled', true);

      const p = isEdit
        ? window.App.AccountService.update(existing.id, payload)
        : window.App.AccountService.create(payload);

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Conta atualizada' : 'Conta criada', 'success');
        // SSE UPSERT will refresh CacheStore → AccountService.onChange → re-render.
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast(err && err.message ? err.message : 'Falha ao salvar conta');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=save]', submit);
  }

  // ── Modal: confirm delete ─────────────────────────────────
  function openDeleteModal(target) {
    const nameHtml = '<strong>' + esc(target.name) + '</strong>';
    const bodyHtml =
      '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
        'Tem certeza que deseja excluir a conta ' + nameHtml + '? ' +
        'Esta ação não pode ser desfeita.' +
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

    const m = window.modal({
      title: 'Excluir Conta',
      body: bodyHtml,
      footer: $footer[0].outerHTML,
    });
    m.open();

    m.$el.on('click', '[data-act=confirm-delete]', function () {
      const $b = $(this).prop('disabled', true);
      window.App.AccountService.remove(target.id).then(function () {
        m.close();
        window.toast('Conta excluída', 'success');
        // SSE DELETE will refresh CacheStore → AccountService.onChange → re-render.
      }).catch(function (err) {
        $b.prop('disabled', false);
        window.toast(err && err.message ? err.message : 'Falha ao excluir conta');
      });
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
  window.Pages['accounts'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      syncFromCache();
      render();
      state.unsubscribe = window.App.AccountService.onChange(function () {
        syncFromCache();
        render();
      });
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.accs');
      }
      if (state && state.unsubscribe) state.unsubscribe();
      state = null;
    }
  };
})();

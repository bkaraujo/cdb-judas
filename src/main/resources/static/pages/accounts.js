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

  function findAccount(id) { return window.byId(state.accounts, id); }

  function colorOf(a) { return a.color || '#6366F1'; }

  function iconForType(type) {
    return type === 'CREDIT_CARD' ? 'creditCard' : 'building';
  }

  function maskInitial(n) {
    return window.maskCurrency(n);
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
      balanceNegative: isEdit ? (Number(existing.balance || 0) < 0) : false,
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

    const swatchesHtml = window.swatchesHtml(DEFAULT_COLORS, initial.color);

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
            '<div style="display:flex;gap:6px;align-items:center;">' +
              '<button type="button" data-act="toggle-balance-sign" ' +
                'style="width:36px;height:36px;border-radius:var(--radius-sm);' +
                'border:1px solid var(--border);background:transparent;cursor:pointer;' +
                'font-size:18px;font-weight:700;flex-shrink:0;line-height:1;' +
                'color:' + (initial.balanceNegative ? 'var(--expense)' : 'var(--text-secondary)') + ';" ' +
                'title="Alternar sinal">' +
                (initial.balanceNegative ? '−' : '+') +
              '</button>' +
              '<input id="' + ids.balance + '" name="balance" type="text" inputmode="numeric" ' +
                'value="' + esc(initial.balance) + '" />' +
            '</div>' +
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

    const m = window.modal({
      title: isEdit ? 'Editar Conta' : 'Nova Conta',
      body: bodyHtml,
      footer: window.saveCancelFooter(),
    });
    m.open();

    const $form = m.$body.find('form[data-form=acc]');
    const $type = $form.find('select[name=type]');
    const $color = $form.find('input[name=color]');
    const $cc = m.$body.find('[data-region=cc-fields]');
    const $balance = $form.find('input[name=balance]');
    const $limit = $form.find('input[name=limit]');

    // Swatch picker. Captures the returned paint(hex) fn so the credit-card
    // auto-color logic below can repaint borders after setting $color.
    const paintSwatches = window.bindSwatches(m, $color);

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

    // Sign toggle for initial balance.
    function updateSignBtn() {
      const $btn = $form.find('[data-act=toggle-balance-sign]');
      $btn.text(initial.balanceNegative ? '−' : '+');
      $btn.css('color', initial.balanceNegative ? 'var(--expense)' : 'var(--text-secondary)');
    }
    $form.on('click', '[data-act=toggle-balance-sign]', function (e) {
      e.preventDefault();
      initial.balanceNegative = !initial.balanceNegative;
      updateSignBtn();
    });

    // Currency masks.
    window.bindCurrencyMask($balance);
    window.bindCurrencyMask($limit);

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
        balance: (window.parseCurrency($balance.val()) * (initial.balanceNegative ? -1 : 1)).toFixed(2),
        type: t,
        color: $color.val() || '#6366F1',
        active: $form.find('input[name=active]').is(':checked'),
      };
      if (t === 'CREDIT_CARD') {
        const linkedId = $form.find('select[name=linkedAccountId]').val();
        if (linkedId) payload.linkedAccountId = linkedId;
        payload.additionalInfo = {
          last4: ($form.find('input[name=last4]').val() || '').trim(),
          limit: window.parseCurrency($limit.val()).toFixed(2),
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
    // Find credit cards linked to this account.
    const linkedCards = state.accounts.filter(function (a) {
      return a.type === 'CREDIT_CARD' && String(a.linkedAccountId) === String(target.id);
    });

    // Collect all account IDs to check (target + linked cards).
    const idsToCheck = [String(target.id)];
    linkedCards.forEach(function (c) { idsToCheck.push(String(c.id)); });

    // Check if any of those accounts have transactions.
    window.App.TransactionService.list('').then(function (all) {
      const allTxs = Array.isArray(all) ? all : [];
      let txCount = 0;
      allTxs.forEach(function (tx) {
        if (idsToCheck.indexOf(String(tx.accountId)) >= 0) txCount++;
      });
      showDeleteConfirm(target, linkedCards, txCount);
    }).catch(function () {
      showDeleteConfirm(target, linkedCards, 0);
    });
  }

  function showDeleteConfirm(target, linkedCards, txCount) {
    const nameHtml = '<strong>' + esc(target.name) + '</strong>';
    let bodyHtml = '';
    const hasWarning = linkedCards.length > 0 || txCount > 0;

    if (hasWarning) {
      const warnings = [];

      if (linkedCards.length > 0) {
        const cardNames = linkedCards.map(function (c) { return c.name; }).join(', ');
        warnings.push(
          'Esta conta possui <strong>' + esc(String(linkedCards.length)) + '</strong> ' +
          (linkedCards.length === 1 ? 'cartão vinculado' : 'cartões vinculados') +
          ' (' + esc(cardNames) + ') que ' +
          (linkedCards.length === 1 ? 'será excluído' : 'serão excluídos') + ' junto com a conta.'
        );
      }

      if (txCount > 0) {
        warnings.push(
          'Existem <strong>' + esc(String(txCount)) + '</strong> transaç' +
          (txCount === 1 ? 'ão vinculada' : 'ões vinculadas') +
          (linkedCards.length > 0 ? ' à conta e seus cartões' : ' a esta conta') +
          ' que ' + (txCount === 1 ? 'será removida' : 'serão removidas') + ' permanentemente.'
        );
      }

      bodyHtml +=
        '<div style="padding:12px 14px;border-radius:8px;background:var(--expense-bg, rgba(244,63,94,0.08));' +
          'border:1px solid var(--expense, #F43F5E);margin-bottom:12px;">' +
          '<p style="font-size:13px;font-weight:600;color:var(--expense, #F43F5E);margin:0 0 6px 0;">' +
            window.icon('alertCircle', 16) + ' Atenção' +
          '</p>' +
          warnings.map(function (w) {
            return '<p style="font-size:13px;color:var(--text-secondary);margin:0 0 4px 0;line-height:1.5;">' + w + '</p>';
          }).join('') +
        '</div>';
    }

    bodyHtml +=
      '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
        'Tem certeza que deseja excluir a conta ' + nameHtml + '? ' +
        'Esta ação ' + (hasWarning ? '<strong>não pode ser desfeita</strong>' : 'não pode ser desfeita') + '.' +
      '</p>';

    window.confirmModal({
      title: 'Excluir Conta',
      body: bodyHtml,
      onConfirm: function (m, reEnable) {
        window.App.AccountService.remove(target.id).then(function () {
          m.close();
          window.toast('Conta excluída', 'success');
          // SSE DELETE will refresh CacheStore → AccountService.onChange → re-render.
        }).catch(function (err) {
          reEnable();
          window.toast(err && err.message ? err.message : 'Falha ao excluir conta');
        });
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

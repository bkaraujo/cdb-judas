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

  // ── Helpers ───────────────────────────────────────────────

  function findAccount(id) { return window.byId(state.accounts, id); }

  function colorOf(a) { return a.color || '#6366F1'; }

  function iconForType(type) {
    return type === 'INVESTMENT' ? 'trendingUp' : 'building';
  }

  function maskInitial(n) {
    return window.maskCurrency(n);
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
      balance: 'acc-balance-' + uniq,
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
      color: isEdit ? (existing.color || '#6366F1') : '#6366F1',
      balance: isEdit ? maskInitial(existing.balance) : '0,00',
      balanceNegative: isEdit ? (Number(existing.balance || 0) < 0) : false,
      active: isEdit ? (existing.active !== false) : true,
      creditLimit: isEdit && existing.creditLimit ? maskInitial(existing.creditLimit) : '',
      overdraftLimit: isEdit && existing.overdraftLimit ? maskInitial(existing.overdraftLimit) : '',
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

    const swatchesHtml = window.swatchesHtml(DEFAULT_COLORS, initial.color);

    function cardsRegionHtml() {
      if (!isEdit) {
        return '<p style="font-size:12px;color:var(--text-muted);">' +
          'Salve a conta antes de adicionar cartões.' +
        '</p>';
      }
      const chips = cards.map(function (c) {
        return '<span class="chip" data-card-id="' + esc(c.id) + '">' +
          '<span>•••• ' + esc(c.last4) + '</span>' +
          '<span class="chip-actions">' +
            '<button type="button" data-act="remove-card" data-id="' + esc(c.id) + '" ' +
              'style="background:none;border:none;cursor:pointer;color:var(--text-muted);' +
              'padding:0;display:inline-flex;" title="Remover cartão">' + window.icon('x', 12) + '</button>' +
          '</span>' +
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

    const m = window.modal({
      title: isEdit ? 'Editar Conta' : 'Nova Conta',
      body: bodyHtml,
      footer: window.saveCancelFooter(),
    });
    m.open();

    const $form = m.$body.find('form[data-form=acc]');
    const $color = $form.find('input[name=color]');
    const $balance = $form.find('input[name=balance]');
    const $limit = $form.find('input[name=creditLimit]');
    const $overdraft = $form.find('input[name=overdraftLimit]');

    window.bindSwatches(m, $color);

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
      const $btn = $(this).prop('disabled', true);
      window.App.AccountService.addCard(existing.id, { last4: last4 }).then(function (card) {
        cards.push(card);
        renderCardsBody();
        window.toast('Cartão adicionado', 'success');
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao adicionar cartão', 'error');
      });
    });

    m.$el.on('click', '[data-act=remove-card]', function () {
      if (!isEdit) return;
      const cardId = $(this).attr('data-id');
      const $chip = $(this).closest('[data-card-id]');
      $chip.css('opacity', '0.5');
      window.App.AccountService.removeCard(existing.id, cardId).then(function () {
        cards = cards.filter(function (c) { return String(c.id) !== String(cardId); });
        renderCardsBody();
        window.toast('Cartão removido', 'success');
      }).catch(function (err) {
        $chip.css('opacity', '1');
        window.toast((err && err.message) || 'Falha ao remover cartão', 'error');
      });
    });

    m.$body.on('keydown', '[data-field=new-card-last4]', function (e) {
      if (e.key === 'Enter') {
        e.preventDefault();
        m.$el.find('[data-act=add-card]').trigger('click');
      }
    });

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($form.find('input[name=name]').val() || '').trim();
      if (!name) {
        $form.find('input[name=name]').trigger('focus');
        return;
      }
      const payload = {
        name: name,
        balance: (window.parseCurrency($balance.val()) * (initial.balanceNegative ? -1 : 1)).toFixed(2),
        type: $form.find('select[name=type]').val(),
        color: $color.val() || '#6366F1',
        active: $form.find('input[name=active]').is(':checked'),
        creditLimit: parseOptionalCurrency($limit.val()),
        overdraftLimit: parseOptionalCurrency($overdraft.val()),
        closingDay: parseOptionalDay($form.find('input[name=closingDay]').val()),
        dueDay: parseOptionalDay($form.find('input[name=dueDay]').val()),
      };

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
    window.App.TransactionService.list('').then(function (all) {
      const allTxs = Array.isArray(all) ? all : [];
      let txCount = 0;
      allTxs.forEach(function (tx) {
        if (String(tx.accountId) === String(target.id)) txCount++;
      });
      showDeleteConfirm(target, txCount);
    }).catch(function () {
      showDeleteConfirm(target, 0);
    });
  }

  function showDeleteConfirm(target, txCount) {
    const nameHtml = '<strong>' + esc(target.name) + '</strong>';
    const cards = target.cards || [];
    let bodyHtml = '';
    const hasWarning = cards.length > 0 || txCount > 0;

    if (hasWarning) {
      const warnings = [];

      if (cards.length > 0) {
        const last4s = cards.map(function (c) { return '•••• ' + c.last4; }).join(', ');
        warnings.push(
          'Esta conta possui <strong>' + esc(String(cards.length)) + '</strong> ' +
          (cards.length === 1 ? 'cartão' : 'cartões') +
          ' (' + esc(last4s) + ') que ' +
          (cards.length === 1 ? 'será excluído' : 'serão excluídos') + ' junto com a conta.'
        );
      }

      if (txCount > 0) {
        warnings.push(
          'Existem <strong>' + esc(String(txCount)) + '</strong> transaç' +
          (txCount === 1 ? 'ão vinculada' : 'ões vinculadas') +
          ' a esta conta que ' + (txCount === 1 ? 'será removida' : 'serão removidas') + ' permanentemente.'
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

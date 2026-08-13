/* feature/transactions.js — fatia Lançamentos (maior fatia, mesma f006 do backend). Um arquivo
 * por fatia: application → infrastructure/secondary → infrastructure/primary (create-edit modal
 * → actions de linha → página), cada bloco abaixo é um IIFE independente (comentário original
 * de cada arquivo preservado como separador de seção). Sem domain própria: Domain.Transaction é
 * kernel (core/kernel/_0_domain/transaction.js) — statement/credit-cards/accounts-payable
 * precisam da forma pura pra renderizar linha de extrato/fatura/conta a pagar.
 *
 * Botão "Importar" abre a fatia import-statement via ImportStatementApi.open (fatia irmã). */

/* application — Transaction use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list(params)                    { return repo.list(params); }

  /* Lançamentos do período com as compras de cartão colapsadas em UMA linha 'Cartões de crédito'
     por vencimento (collapse + mergeCards — ver Domain.Invoice). A busca usa a janela alargada (o
     ciclo da fatura começa no mês anterior), por isso `raw` traz mais do que o período: é o índice
     cru que os modais de editar/excluir usam para resolver a transação por trás de uma linha. */
  function listForPeriod(period) {
    const accounts = cache.accounts();
    const w = window.Domain.Invoice.fetchWindow(accounts, period);
    return repo.list('dateFrom=' + w.from + '&dateTo=' + w.to).then(function (list) {
      const raw = Array.isArray(list) ? list : [];
      const rows = window.Domain.Invoice.collapse(raw, accounts, period);
      return { rows: window.Domain.Invoice.mergeCards(rows, accounts), raw: raw };
    });
  }
  function listByAccount(accountId, params){ return repo.listByAccount(accountId, params); }
  function create(data)                    { return repo.create(data); }
  function update(id, data)                { return repo.update(id, data); }
  function patchStatus(accountId, id, status, date) { return repo.patchStatus(accountId, id, status, date); }
  function remove(accountId, id, scope)    { return repo.remove(accountId, id, scope); }
  function transfer(data)                  { return repo.transfer(data); }
  function importPreview(file, password, accountId) { return repo.importPreview(file, password, accountId); }
  function importConfirm(data)             { return repo.importConfirm(data); }

  /* Builds payload with the domain signing rule applied to amount. */
  function buildPayload(form) {
    const type = window.Domain.Transaction.normalizeType(form.type);
    return Object.assign({}, form, {
      type: type,
      amount: window.Domain.Transaction.signedAmount(type, form.amount),
    });
  }

  window.App = window.App || {};
  window.App.TransactionService = {
    init: init,
    list: list,
    listForPeriod: listForPeriod,
    listByAccount: listByAccount,
    create: create,
    update: update,
    patchStatus: patchStatus,
    remove: remove,
    transfer: transfer,
    importPreview: importPreview,
    importConfirm: importConfirm,
    buildPayload: buildPayload,
  };
})();

/* _3_infrastructure/secondary/transaction-repository.js — HTTP adapter for transactions
 * under the accounts namespace: /api/{uuid}/accounts/... */
(function () {
  function create(http) {
    return {
      list:        function (params) { return http.get('/accounts/transactions' + (params ? '?' + params : '')); },
      listByAccount: function (accountId, params) {
        return http.get('/accounts/' + accountId + '/transactions' + (params ? '?' + params : ''));
      },
      create:      function (data)   { return http.post('/accounts/' + data.accountId + '/transactions', data); },
      update:      function (id, d)  { return http.patch('/accounts/' + d.accountId + '/transactions/' + id, d); },
      patchStatus: function (accountId, id, status, paymentDate) {
        return http.patch('/accounts/' + accountId + '/transactions/' + id + '/status', { status: status, paymentDate: paymentDate });
      },
      remove:      function (accountId, id, mode) {
        return http.delete('/accounts/' + accountId + '/transactions/' + id + (mode ? '?mode=' + mode : ''));
      },
      transfer:    function (data)   { return http.post('/accounts/transactions/transfer', data); },
      importPreview: function (file, password, accountId) {
        const fd = new FormData();
        fd.append('file', file);
        if (password) fd.append('password', password);
        if (accountId) fd.append('accountId', accountId);
        return http.upload('/accounts/transactions/import/preview', fd);
      },
      importConfirm: function (data) { return http.post('/accounts/transactions/import/confirm', data); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.TransactionRepository = { create: create };
})();

/* pages/transactions/create-edit.js — Diálogo de criação/edição de lançamento.
 *
 * Modal único para criar ou editar um lançamento (despesa/receita/transferência),
 * incluindo o quick-create de categoria a partir do próprio formulário.
 *
 * Uso:
 *   window.transactionFormModal({
 *     existing: tx | null,        // null = novo lançamento
 *     isTransfer: Boolean,        // se o tx em edição é uma transferência (par income/expense)
 *     defaultDate: 'YYYY-MM-DD',  // data padrão para um novo lançamento
 *     onSaved: function () { return loadTransactions(); },
 *   });
 *
 * As dependências de estado da página (detecção de transferência, data padrão,
 * recarregar a lista) vêm via opts; o resto usa helpers globais (esc, flatCategories,
 * accountsList, window.App.*).
 */
(function () {
  window.transactionFormModal = function (opts) {
    opts = opts || {};
    const existing = opts.existing || null;
    const onSaved = typeof opts.onSaved === 'function'
      ? opts.onSaved
      : function () { return null; };

    const isEdit = !!existing;
    const isTransferEdit = isEdit && !!opts.isTransfer;
    const uniq = Date.now();
    const ids = {
      desc: 'tx-desc-' + uniq,
      amount: 'tx-amount-' + uniq,
      date: 'tx-date-' + uniq,
      category: 'tx-cat-' + uniq,
      account: 'tx-acc-' + uniq,
      card: 'tx-card-' + uniq,
      destAccount: 'tx-dest-' + uniq,
      costCenter: 'tx-cc-' + uniq,
      status: 'tx-status-' + uniq,
      notes: 'tx-notes-' + uniq,
    };

    const initial = {
      type: isEdit ? (existing.type || 'expense') : 'expense',
      description: isEdit ? (existing.description || '') : '',
      amount: isEdit ? maskCurrency(String(Math.round(Math.abs(Number(existing.amount) || 0) * 100) / 100)) : '',
      date: isEdit ? (existing.date || '').slice(0, 10) : opts.defaultDate,
      categoryId: isEdit ? (existing.categoryId || '') : '',
      accountId: isEdit ? String(existing.accountId || '') : '',
      destAccountId: '',
      status: isEdit ? (existing.status === 'balance' ? 'confirmed' : (existing.status || 'confirmed')) : 'confirmed',
      costCenterId: isEdit ? String(existing.costCenterId || '') : '',
      cardId: isEdit ? (existing.cardId || '') : '',
      isEstorno: isEdit ? (
        (existing.type === 'expense' && Number(existing.amount) > 0) ||
        (existing.type === 'income'  && Number(existing.amount) < 0)
      ) : false,
      notes: isEdit ? (existing.notes || '') : '',
      tagIds: isEdit ? (existing.tagIds || []).map(String) : [],
    };

    // Account options.
    const accs = accountsList().filter(function (a) { return a.active !== false || (isEdit && String(a.id) === initial.accountId); });
    if (!initial.accountId && accs.length) initial.accountId = String(accs[0].id);

    function natureForType(t) {
      if (t === 'income') return 'INCOME';
      if (t === 'expense') return 'EXPENSE';
      return null;
    }

    // Perna de transferência: "Conta" é renomeada para orientar qual lado ela representa —
    // EXPENSE = saída (Origem), INCOME = entrada (Destino). Só depende da natureza da própria
    // perna, não da conta irmã.
    function accountFieldLabel() {
      if (!isTransferEdit) return 'Conta';
      return initial.type === 'expense' ? 'Conta de Origem' : 'Conta de Destino';
    }

    // Item list backing both the hidden native <select> (mantém toda leitura/escrita existente
    // via .val()/option[value]) e o overlay de busca (`window.searchSelectHtml`) — uma única
    // fonte pra não deixar os dois desalinhar.
    function buildCatItems(type, selectedId) {
      const nat = natureForType(type);
      const cats = flatCategories(nat, true, selectedId);
      if (!cats.length) return [{ value: '', label: 'Nenhuma categoria disponível' }];
      return cats.map(function (c) { return { value: String(c.id), label: c.label }; });
    }

    function buildAccountOpts(selectedId, includeEmpty) {
      if (!accs.length) return '<option value="">Nenhuma conta disponível</option>';
      const empty = includeEmpty ? '<option value="">— Selecione —</option>' : '';
      return empty + accs.map(function (a) {
        const sel = String(a.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
      }).join('');
    }

    // Card select is optional and only relevant when the chosen account has
    // cards attached; options + visibility are recomputed whenever the account
    // selection changes (see the accountId `change` handler below).
    function cardOptionsHtml(accountId, selectedCardId) {
      const acc = window.byId(accs, accountId);
      if (!acc || !window.Domain.Account.hasCards(acc)) return '';
      const visible = acc.cards.filter(function (c) {
        return c.active !== false || String(c.id) === String(selectedCardId);
      });
      if (!visible.length) return '';
      return '<option value="">— Nenhum —</option>' + visible.map(function (c) {
        const sel = String(c.id) === String(selectedCardId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>•••• ' + esc(c.last4) + '</option>';
      }).join('');
    }

    function cardFieldHtml(accountId, selectedCardId) {
      const options = cardOptionsHtml(accountId, selectedCardId);
      return '<div class="form-group" data-region="card-field" style="display:' + (options ? 'block' : 'none') + ';">' +
        '<label class="form-label" for="' + ids.card + '">Cartão</label>' +
        '<select id="' + ids.card + '" name="cardId"' + (isTransferEdit ? ' disabled' : '') + '>' + options + '</select>' +
      '</div>';
    }

    function buildCostCenterOpts(selectedId) {
      const ccs = window.App.CacheStore.costCenters();
      if (!ccs.length) return '<option value="">Nenhum centro de custo</option>';
      const variavel = ccs.filter(function (c) { return /vari/i.test(c.description || c.name || ''); })[0];
      const target = selectedId || (variavel && variavel.id) || '';
      return ccs.map(function (c) {
        const label = c.description || c.name || '';
        const sel = String(c.id) === String(target) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(label) + '</option>';
      }).join('');
    }

    const accOpts = buildAccountOpts(initial.accountId, false);

    const statusOpts = [
      ['confirmed', 'Confirmado'],
      ['pending', 'Pendente'],
      ['scheduled', 'Agendado'],
    ].map(function (p) {
      const sel = p[0] === initial.status ? ' selected' : '';
      return '<option value="' + esc(p[0]) + '"' + sel + '>' + esc(p[1]) + '</option>';
    }).join('');

    function typeBtnHtml(val, label, color, active, disabled) {
      return '<button type="button" data-act="set-form-type" data-type="' + esc(val) + '" ' +
        (disabled ? 'disabled title="Tipo não pode ser alterado ao editar uma perna de transferência" ' : '') +
        'style="flex:1;padding:8px;border-radius:var(--radius-sm);font-size:13px;font-weight:600;' +
        'border:1px solid ' + (active ? 'var(--' + color + ')' : 'var(--border)') + ';' +
        'background:' + (active ? 'var(--' + color + '-light)' : 'transparent') + ';' +
        'color:' + (active ? 'var(--' + color + ')' : 'var(--text-secondary)') + ';' +
        'opacity:' + (disabled ? '0.55' : '1') + ';' +
        'cursor:' + (disabled ? 'not-allowed' : 'pointer') + ';transition:all var(--transition);">' +
        esc(label) +
      '</button>';
    }

    function buildGridHtml(type) {
      if (type === 'transfer') {
        return (
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.account + '">Origem</label>' +
            '<select id="' + ids.account + '" name="accountId">' + buildAccountOpts(initial.accountId, true) + '</select>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.destAccount + '">Destino</label>' +
            '<select id="' + ids.destAccount + '" name="destAccountId">' + buildAccountOpts(initial.destAccountId, true) + '</select>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.date + '">Data</label>' +
            '<input id="' + ids.date + '" name="date" type="date" required value="' + esc(initial.date) + '" />' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label" for="' + ids.amount + '">Valor (R$)</label>' +
            '<input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" ' +
              'placeholder="0,00" value="' + esc(initial.amount) + '" />' +
          '</div>'
        );
      }
      return (
        '<div class="form-group full">' +
          '<label class="form-label" for="' + ids.desc + '">Descrição</label>' +
          '<input id="' + ids.desc + '" name="description" type="text" required ' +
            (isTransferEdit ? 'disabled ' : '') +
            'style="text-transform:uppercase;" ' +
            'placeholder="Ex: Mercado, Salário..." value="' + esc(initial.description) + '" />' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.date + '">Data</label>' +
          '<input id="' + ids.date + '" name="date" type="date" required value="' + esc(initial.date) + '" />' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.account + '">' + esc(accountFieldLabel()) + '</label>' +
          '<select id="' + ids.account + '" name="accountId">' + accOpts + '</select>' +
        '</div>' +
        cardFieldHtml(initial.accountId, initial.cardId) +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.costCenter + '">Centro de Custo</label>' +
          '<select id="' + ids.costCenter + '" name="costCenterId"' + (isTransferEdit ? ' disabled' : '') + '>' +
            buildCostCenterOpts(initial.costCenterId) +
          '</select>' +
        '</div>' +
        '<div class="form-group">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">' +
            '<label class="form-label" for="' + ids.category + '" style="margin:0;">Categoria</label>' +
            '<button type="button" data-act="new-category" ' +
              'style="background:none;border:none;color:var(--accent);cursor:pointer;' +
              'font-size:11px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' +
              window.icon('plus', 12) + 'Nova categoria' +
            '</button>' +
          '</div>' +
          (function () {
            var catItems = buildCatItems(type, initial.categoryId);
            return window.categoryPickerHtml({
              items: catItems,
              selectedId: initial.categoryId,
              selectId: ids.category,
              selectAttrs: ' name="categoryId" data-region="category-select"',
              placeholder: 'Selecione',
            });
          })() +
        '</div>' +
        '<div class="form-group">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">' +
            '<label class="form-label" style="margin:0;">Tags</label>' +
            '<button type="button" data-act="new-tag" ' +
              'style="background:none;border:none;color:var(--accent);cursor:pointer;' +
              'font-size:11px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' +
              window.icon('plus', 12) + 'Nova tag' +
            '</button>' +
          '</div>' +
          window.tagsDropdownHtml(initial.tagIds, 'tx') +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.status + '">Status</label>' +
          '<select id="' + ids.status + '" name="status">' + statusOpts + '</select>' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.amount + '">Valor (R$)</label>' +
          '<input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" ' +
            'placeholder="0,00" value="' + esc(initial.amount) + '" />' +
        '</div>' +
        '<div class="form-group full">' +
          '<label style="display:inline-flex;align-items:center;gap:8px;' + (isTransferEdit ? 'cursor:not-allowed;opacity:0.55;' : 'cursor:pointer;') +
            'font-size:13px;font-weight:500;color:var(--text-secondary);">' +
            '<input type="checkbox" name="estorno"' + (initial.isEstorno ? ' checked' : '') +
              (isTransferEdit ? ' disabled' : '') +
              ' style="width:16px;height:16px;accent-color:var(--accent);" />' +
            'Estorno (inverter sinal)' +
          '</label>' +
        '</div>' +
        '<div class="form-group full">' +
          '<label class="form-label" for="' + ids.notes + '">Anotações <span style="font-weight:400;color:var(--text-muted);">(opcional)</span></label>' +
          '<textarea id="' + ids.notes + '" name="notes" maxlength="250" rows="3" ' +
            (isTransferEdit ? 'disabled ' : '') +
            'placeholder="Até 250 caracteres..." style="resize:vertical;min-height:60px;">' + esc(initial.notes) + '</textarea>' +
          '<p style="font-size:11px;color:var(--text-muted);margin-top:4px;text-align:right;" data-region="notes-counter">' +
            esc(initial.notes.length + '/250') +
          '</p>' +
        '</div>'
      );
    }

    const bodyHtml =
      '<form data-form="tx" autocomplete="off">' +
        (isTransferEdit
          ? '<div style="display:flex;gap:8px;align-items:flex-start;padding:10px 12px;margin-bottom:14px;' +
              'border:1px solid var(--info);border-radius:var(--radius-sm);background:var(--info-light, transparent);">' +
              '<span style="color:var(--info);flex-shrink:0;display:flex;">' + window.icon('alertCircle', 16) + '</span>' +
              '<span style="font-size:12px;color:var(--text-secondary);line-height:1.4;">' +
                'Esta transação faz parte de uma transferência. Você pode ajustar conta, data, valor, ' +
                'status e categoria desta perna — a perna oposta é sincronizada automaticamente. Tipo e ' +
                'os demais campos não podem ser alterados por aqui.' +
              '</span>' +
            '</div>'
          : '') +
        '<div data-region="type-row" style="display:flex;gap:8px;margin-bottom:16px;">' +
          typeBtnHtml('expense',  '↓ Despesa',       'expense',  initial.type === 'expense', isTransferEdit) +
          typeBtnHtml('income',   '↑ Receita',       'income',   initial.type === 'income',  isTransferEdit) +
          typeBtnHtml('transfer', '⇄ Transferência', 'accent',   initial.type === 'transfer', isTransferEdit) +
        '</div>' +
        '<input type="hidden" name="type" value="' + esc(initial.type) + '" />' +
        '<div class="form-grid" data-region="grid">' +
          buildGridHtml(initial.type) +
        '</div>' +
      '</form>';

    const m = window.modal({
      title: isEdit ? 'Editar Lançamento' : 'Novo Lançamento',
      body: bodyHtml,
      footer: window.saveCancelFooter(),
    });
    m.open();

    const $form = m.$body.find('form[data-form=tx]');
    function bindAmountMask() {
      if (window.bindCurrencyMask) window.bindCurrencyMask($form.find('input[name=amount]'));
    }
    bindAmountMask();

    // Live character counter for the notes textarea.
    function bindNotesCounter() {
      var $notes = $form.find('textarea[name=notes]');
      if ($notes.length) {
        $notes.off('input.notesCnt').on('input.notesCnt', function () {
          var len = (this.value || '').length;
          m.$body.find('[data-region=notes-counter]').text(len + '/250');
        });
      }
    }
    bindNotesCounter();

    // Live "regra de nomenclatura" match: while typing the description, if a rule's name appears
    // in the text, replace the description with the rule's own name and pre-fill whichever of
    // conta/categoria/centro de custo the rule sets — same widget, still editable by hand
    // afterwards. Delegated on m.$body (survives the grid rebuild on type change) and idempotent
    // (once the field equals the rule's name, re-matching is a no-op, so it never fights further
    // manual edits to the other fields).
    m.$body.on('input', 'input[name=description]', function () {
      const $desc = $(this);
      const value = $desc.val() || '';
      const rule = window.ImportRulesApi.match(value, window.ImportRulesApi.listCached());
      if (!rule || value === rule.name) return;

      $desc.val(rule.name);
      initial.description = rule.name;

      if (rule.accountId) {
        const $acc = m.$body.find('select[name=accountId]');
        if ($acc.find('option[value="' + esc(rule.accountId) + '"]').length) {
          $acc.val(String(rule.accountId)).trigger('change');
          initial.accountId = String(rule.accountId);
        }
      }
      if (rule.categoryId) {
        const $cat = m.$body.find('select[name=categoryId]');
        if ($cat.find('option[value="' + esc(rule.categoryId) + '"]').length) {
          $cat.val(String(rule.categoryId));
          window.refreshSearchSelect(ids.category);
          initial.categoryId = String(rule.categoryId);
        }
      }
      if (rule.costCenterId) {
        const $cc = m.$body.find('select[name=costCenterId]');
        if ($cc.find('option[value="' + esc(rule.costCenterId) + '"]').length) {
          $cc.val(String(rule.costCenterId));
          initial.costCenterId = String(rule.costCenterId);
        }
      }
    });

    // Tags: mesma dropdown compartilhada do import (ui.js) — checkboxes fora de qualquer <select>,
    // então o estado mora em initial.tagIds (mutado direto a cada change), não lido do DOM no submit.
    m.$body.on('change', '[data-tag-check]', function () {
      const tagId = String($(this).attr('data-tag-id'));
      const current = initial.tagIds.map(String);
      initial.tagIds = this.checked ? current.concat([tagId]) : current.filter(function (id) { return id !== tagId; });
      window.refreshTagsDropdownLabel($(this));
    });

    // Account change refreshes which cards are offered (a card belongs to one account).
    m.$body.on('change', 'select[name=accountId]', function () {
      const $field = m.$body.find('[data-region=card-field]');
      if (!$field.length) return;
      const options = cardOptionsHtml($(this).val(), null);
      $field.find('select[name=cardId]').html(options);
      $field.css('display', options ? 'block' : 'none');
    });

    // Type buttons sync hidden input + rebuild grid layout for the chosen type.
    m.$body.on('click', '[data-act=set-form-type]', function (e) {
      e.preventDefault();
      if (isTransferEdit) return; // tipo é imutável ao editar uma perna de transferência (par se desfaria)
      const t = $(this).attr('data-type');
      // Snapshot current values to preserve across rebuild.
      initial.description = $form.find('input[name=description]').val() || initial.description;
      initial.amount      = $form.find('input[name=amount]').val()      || initial.amount;
      initial.date        = $form.find('input[name=date]').val()        || initial.date;
      initial.accountId   = $form.find('select[name=accountId]').val()  || initial.accountId;
      const $destSel      = $form.find('select[name=destAccountId]');
      if ($destSel.length) initial.destAccountId = $destSel.val() || initial.destAccountId;
      const $catSel       = $form.find('select[name=categoryId]');
      if ($catSel.length)  initial.categoryId    = $catSel.val()       || initial.categoryId;
      const $stSel        = $form.find('select[name=status]');
      if ($stSel.length)   initial.status        = $stSel.val()        || initial.status;
      const $ccSel        = $form.find('select[name=costCenterId]');
      if ($ccSel.length)   initial.costCenterId  = $ccSel.val()        || initial.costCenterId;
      const $cardSel      = $form.find('select[name=cardId]');
      if ($cardSel.length) initial.cardId        = $cardSel.val()      || initial.cardId;
      const $estornoChk   = $form.find('input[name=estorno]');
      if ($estornoChk.length) initial.isEstorno  = $estornoChk.is(':checked');
      const $notesTa      = $form.find('textarea[name=notes]');
      if ($notesTa.length) initial.notes         = $notesTa.val()       || initial.notes;

      $form.find('input[name=type]').val(t);
      // Repaint type row.
      const $row = m.$body.find('[data-region=type-row]');
      $row.empty();
      $row.append(typeBtnHtml('expense',  '↓ Despesa',       'expense',  t === 'expense', isTransferEdit));
      $row.append(typeBtnHtml('income',   '↑ Receita',       'income',   t === 'income',  isTransferEdit));
      $row.append(typeBtnHtml('transfer', '⇄ Transferência', 'accent',   t === 'transfer', isTransferEdit));
      // Rebuild grid.
      m.$body.find('[data-region=grid]').html(buildGridHtml(t));
      bindAmountMask();
      bindNotesCounter();
    });

    // "+ Nova categoria": quick-create a category inline, then select it.
    m.$body.on('click', '[data-act=new-category]', function (e) {
      e.preventDefault();
      const nature = natureForType($form.find('input[name=type]').val());
      if (!nature) return; // transfer has no category
      window.openCategoryCreateModal(nature, function (created) {
        const $sel = $form.find('select[name=categoryId]');
        // Drop the "Nenhuma categoria disponível" placeholder, if present.
        $sel.find('option').each(function () { if (!this.value) $(this).remove(); });
        if (!$sel.find('option[value="' + esc(created.id) + '"]').length) {
          $sel.append('<option value="' + esc(created.id) + '">' +
            esc(window.quickCategoryLabel(created)) + '</option>');
        }
        $sel.val(String(created.id));
        window.refreshSearchSelect(ids.category);
        initial.categoryId = String(created.id);
      });
    });

    // "+ Nova tag": quick-create a tag inline, then mark it selected.
    m.$body.on('click', '[data-act=new-tag]', function (e) {
      e.preventDefault();
      window.openTagCreateModal(function (created) {
        const id = String(created.id);
        if (initial.tagIds.indexOf(id) === -1) initial.tagIds.push(id);
        window.appendTagRow('tx', created);
      });
    });

    function submit(e) {
      if (e) e.preventDefault();
      const type = $form.find('input[name=type]').val();
      const amtRaw = $form.find('input[name=amount]').val();
      const amt = window.parseCurrency(amtRaw);
      const date = $form.find('input[name=date]').val();
      const accountId = $form.find('select[name=accountId]').val();
      const $btn = m.$el.find('[data-act=save]');

      if (type === 'transfer') {
        const destAccountId = $form.find('select[name=destAccountId]').val();
        if (!accountId) { window.toast('Selecione a conta de origem', 'error'); return; }
        if (!destAccountId) { window.toast('Selecione a conta de destino', 'error'); return; }
        if (!window.Domain.Transaction.isValidTransfer(accountId, destAccountId)) {
          window.toast('Origem e destino devem ser contas diferentes', 'error'); return;
        }
        if (!date) { window.toast('Informe a data', 'error'); return; }
        if (!isFinite(amt) || amt <= 0) { $form.find('input[name=amount]').trigger('focus'); return; }

        $btn.prop('disabled', true);
        window.App.TransactionService.transfer({
          fromAccountId: accountId,
          toAccountId: destAccountId,
          date: date,
          amount: Number(amt.toFixed(2)),
        }).then(function () {
          // Converting an existing lançamento into a transfer: drop the original
          // so its amount isn't counted twice alongside the new transfer pair.
          if (isEdit) return window.App.TransactionService.remove(existing.accountId, existing.id);
        }).then(function () {
          m.close();
          window.toast(isEdit ? 'Lançamento convertido em transferência' : 'Transferência registrada', 'success');
          return onSaved();
        }).catch(function (err) {
          $btn.prop('disabled', false);
          window.toast((err && err.message) || 'Falha ao registrar transferência', 'error');
        });
        return;
      }

      const description = ($form.find('input[name=description]').val() || '').trim();
      if (!description) { $form.find('input[name=description]').trigger('focus'); return; }
      if (!isFinite(amt) || amt <= 0) { $form.find('input[name=amount]').trigger('focus'); return; }
      const categoryId = $form.find('select[name=categoryId]').val();
      const costCenterId = $form.find('select[name=costCenterId]').val();
      const status = $form.find('select[name=status]').val();
      const isEstorno = $form.find('input[name=estorno]').is(':checked');

      if (!accountId) { window.toast('Selecione uma conta', 'error'); return; }
      if (!categoryId) { window.toast('Selecione uma categoria', 'error'); return; }
      if (!costCenterId) { window.toast('Selecione o centro de custo', 'error'); return; }

      const signed = window.Domain.Transaction.signedAmount(type, amt) * (isEstorno ? -1 : 1);
      const notes = ($form.find('textarea[name=notes]').val() || '').trim() || null;
      const payload = {
        description: description,
        amount: Number(signed.toFixed(2)),
        date: date,
        categoryId: categoryId || null,
        accountId: accountId,
        costCenterId: costCenterId,
        status: status,
        type: type,
        notes: notes,
        cardId: $form.find('select[name=cardId]').val() || null,
        tagIds: initial.tagIds,
      };

      $btn.prop('disabled', true);
      const p = isEdit
        ? window.App.TransactionService.update(existing.id, payload)
        : window.App.TransactionService.create(payload);

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Lançamento atualizado' : 'Lançamento criado', 'success');
        return onSaved();
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao salvar lançamento', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=save]', submit);

    return m;
  };
})();

/* pages/transactions/actions.js — Ações de linha de lançamento (transfer detection,
 * exclusão com escopo de grupo, confirmar pagamento), compartilhadas entre a tela de
 * Lançamentos (transactions.js) e o Extrato de Contas (statement.js).
 *
 * Todas recebem a lista de transações do período (`list`) para detectar transferências
 * — uma transferência é um par income/expense no mesmo grupo — e um callback de recarga
 * (`onDone`/`onSaved`) que a página passa para atualizar sua própria visão após a mutação.
 * O modal de criação/edição vive em create-edit.js (window.transactionFormModal).
 */
(function () {
  // A transfer is stored as two legs (one income + one expense) sharing a groupId, unlike
  // installments whose legs share a single type. Both legs carry the same date, so they sit in
  // the same month view together — detection works off the loaded list.
  function isTransfer(tx, list) {
    if (!tx || !tx.groupId) return false;
    const group = (list || []).filter(function (t) { return String(t.groupId) === String(tx.groupId); });
    const hasIncome = group.some(function (t) { return t.type === 'income'; });
    const hasExpense = group.some(function (t) { return t.type === 'expense'; });
    return hasIncome && hasExpense;
  }

  // Create/edit modal (create-edit.js); transfer detection + default date + reload are
  // supplied by the caller so this stays page-agnostic.
  function openFormModal(opts) {
    opts = opts || {};
    window.transactionFormModal({
      existing: opts.existing || null,
      isTransfer: isTransfer(opts.existing || null, opts.list),
      defaultDate: opts.defaultDate,
      onSaved: typeof opts.onSaved === 'function' ? opts.onSaved : function () { return null; },
    });
  }

  // ── Delete (with scope for grouped/recurring) ─────────────
  function openDeleteModal(tx, opts) {
    opts = opts || {};
    const list = opts.list || [];
    const onDone = typeof opts.onDone === 'function' ? opts.onDone : function () { return null; };

    const transfer = isTransfer(tx, list);
    // Transfer legs carry a groupId but are not installments — never offer the parcelas scope for them.
    const isGrouped = !!tx.groupId && !transfer;

    if (!isGrouped) {
      // Simple confirm. A transfer always removes both legs server-side.
      const msg = transfer
        ? 'Excluir esta transferência? As duas pernas (saída e entrada) serão removidas. Esta ação não pode ser desfeita.'
        : 'Excluir <strong>' + window.esc(tx.description || 'lançamento') + '</strong>? Esta ação não pode ser desfeita.';
      window.confirmModal({
        title: transfer ? 'Excluir Transferência' : 'Excluir Lançamento',
        body: window.modalText(msg),
        onConfirm: function (m, reEnable) {
          window.App.TransactionService.remove(tx.accountId, tx.id).then(function () {
            m.close();
            window.toast('Lançamento excluído', 'success');
            return onDone();
          }).catch(function (err) {
            reEnable();
            window.toast((err && err.message) || 'Falha ao excluir', 'error');
          });
        },
      });
      return;
    }

    // Grouped: scope choice. Nomeia a parcela — escolher "este e seguintes" sem saber qual das N
    // está em jogo é decidir no escuro.
    const bodyHtml =
      '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
        '<strong>' + window.esc(window.Domain.Transaction.describe(tx) || 'Lançamento') + '</strong>' +
        ' faz parte de um grupo de parcelas. Como deseja excluí-lo?' +
      '</p>';
    const $only = window.btn({
      variant: 'secondary', size: 'md', label: 'Apenas este',
      attrs: 'data-act="del-single" type="button"'
    });
    const $future = window.btn({
      variant: 'secondary', size: 'md', label: 'Este e seguintes',
      attrs: 'data-act="del-future" type="button"'
    });
    const $all = window.btn({
      variant: 'danger', size: 'md', icon: 'trash', label: 'Todos',
      attrs: 'data-act="del-all" type="button"'
    });
    const $cancel = window.btn({
      variant: 'ghost', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $footer = window.modalFooter([$cancel, $only, $future, $all], { align: 'end' });

    const m = window.modal({
      title: 'Excluir Lançamento Recorrente',
      body: bodyHtml,
      footer: $footer,
    });
    m.open();

    function doRemove(mode) {
      const $btns = m.$el.find('button').prop('disabled', true);
      window.App.TransactionService.remove(tx.accountId, tx.id, mode).then(function () {
        m.close();
        window.toast('Lançamento(s) excluído(s)', 'success');
        return onDone();
      }).catch(function (err) {
        $btns.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao excluir', 'error');
      });
    }
    m.$el.on('click', '[data-act=del-single]', function () { doRemove('SINGLE'); });
    m.$el.on('click', '[data-act=del-future]', function () { doRemove('FUTURE'); });
    m.$el.on('click', '[data-act=del-all]',    function () { doRemove('ALL'); });
  }

  // ── Quick mark as paid ────────────────────────────────────
  function markPaid(tx, opts) {
    opts = opts || {};
    const onDone = typeof opts.onDone === 'function' ? opts.onDone : function () { return null; };
    const today = new Date().toISOString().slice(0, 10);
    window.App.TransactionService.patchStatus(tx.accountId, tx.id, 'confirmed', today).then(function () {
      window.toast('Lançamento confirmado', 'success');
      return onDone();
    }).catch(function (err) {
      window.toast((err && err.message) || 'Falha ao confirmar', 'error');
    });
  }

  window.transactionActions = {
    isTransfer: isTransfer,
    openFormModal: openFormModal,
    openDeleteModal: openDeleteModal,
    markPaid: markPaid,
  };
})();

/* pages/transactions.js — Lançamentos (list + filtros + CRUD modal + status).
 * Card da lista no leiaute de linha do Extrato de Contas: data | conta (coluna fixa) |
 * categoria (coluna fixa) | descrição | badge de status | valor | ações.
 *
 * Compra de cartão não aparece linha a linha nem cartão a cartão: App.TransactionService
 * .listForPeriod colapsa as compras em fatura (Domain.Invoice.collapse) e funde as faturas da mesma
 * conta numa linha só (Domain.Invoice.mergeCards) — 'Cartões <conta>', uma por (conta, vencimento),
 * lançada na data de vencimento e clicável para #/credit-cards. Consequências:
 *   - a linha de cartões é derivada: sem editar/excluir/confirmar;
 *   - os filtros de CATEGORIA e STATUS a escondem — ela agrega lançamentos de várias categorias e
 *     não tem status próprio, então nenhum valor único a representa;
 *   - conta, tipo e busca funcionam normalmente (a busca casa o rótulo);
 *   - os cards de resumo passam a somar a fatura no mês do vencimento.
 * `state.raw` guarda as transações cruas da janela buscada — é delas que os modais de
 * editar/excluir resolvem o lançamento por trás de uma linha. */
(function () {
  window.Pages = window.Pages || {};

  // ── Constants ─────────────────────────────────────────────
  const STATUS_LABEL = {
    confirmed: 'Confirmado',
    pending: 'Pendente',
    scheduled: 'Agendado',
    planed: 'Planejado',
    balance: 'Saldo'
  };
  // Per task spec: pending=expense, scheduled=warning, confirmed=income.
  const STATUS_BADGE = {
    confirmed: 'income',
    pending: 'expense',
    scheduled: 'warning',
    planed: 'info',
    balance: 'muted'
  };

  // ── State ─────────────────────────────────────────────────
  let state = null;

  function resetState() {
    const p = window.App.PeriodService.get(); // { month: 1-12, year }
    state = {
      $root: null,
      loading: true,
      transactions: [],         // linhas exibidas (compras de cartão já colapsadas em fatura)
      raw: [],                  // transações cruas da janela — resolve editar/excluir por id
      month: p.month - 1,       // 0-11
      year: p.year,
      search: '',
      filterType: 'all',           // 'all'|'income'|'expense'|'transfer'
      filterAccount: '',
      filterCategory: '',
      filterStatus: '',
      showFilters: false,
      pendingScrollTop: null,
      pendingRootScrollTop: null,
    };
  }

  // ── Helpers ───────────────────────────────────────────────

  function findTx(id) { return window.byId(state.raw, id); }

  // Default date for a new transaction: today when the displayed month is the
  // current month, otherwise the first day of the month being displayed.
  function defaultNewDate() {
    const now = new Date();
    if (state.month === now.getMonth() && state.year === now.getFullYear()) {
      return now.toISOString().slice(0, 10);
    }
    return window.monthBounds(state.month, state.year).from;
  }

  function openFormModal(existing) {
    window.transactionActions.openFormModal({
      existing: existing || null,
      list: state.raw,
      defaultDate: defaultNewDate(),
      onSaved: function () { return loadTransactions(); },
    });
  }

  // ── Fetch ─────────────────────────────────────────────────
  function loadTransactions() {
    // render() rebuilds the whole page from scratch (`$root.empty()`), which momentarily leaves
    // both scroll containers empty — the browser clamps scrollTop to 0 right there and it never
    // comes back on its own once content regrows. Two containers can scroll independently
    // depending on viewport height: the list card itself (`max-height: calc(100vh - 300px)`) and
    // `#page`/`$root` (`.page-content`, `overflow-y:auto`) when the whole page overflows it.
    // Capture both here; render() re-applies them once the real list is back.
    const $listCard = state.$root && state.$root.find('[data-region=list-card]');
    state.pendingScrollTop = ($listCard && $listCard.length) ? $listCard.scrollTop() : null;
    state.pendingRootScrollTop = state.$root ? state.$root.scrollTop() : null;

    state.loading = true;
    render();
    const period = window.Domain.Period.create(state.month + 1, state.year);
    return window.App.TransactionService.listForPeriod(period).then(function (res) {
      state.transactions = res.rows;
      state.raw = res.raw;
      state.loading = false;
      render();
    }).catch(function (err) {
      state.loading = false;
      state.transactions = [];
      state.raw = [];
      render();
      window.toast((err && err.message) || 'Falha ao carregar lançamentos', 'error');
    });
  }

  // ── Filtering / Summary ───────────────────────────────────
  function filteredTxs() {
    const catMap = categoryById();
    const s = (state.search || '').toLowerCase();
    return state.transactions.filter(function (tx) {
      // A linha de cartões agrega várias categorias e não tem status próprio: filtrar por um
      // desses valores a exclui em vez de fingir que ela tem um. (Conta ela tem — é uma linha por
      // conta —, então o filtro de conta funciona normalmente.)
      if (tx.invoice && (state.filterCategory || state.filterStatus)) return false;
      const cat = catMap[tx.categoryId];
      const catLbl = cat ? categoryLabel(cat).toLowerCase() : '';
      const matchSearch = !s ||
        (tx.description || '').toLowerCase().indexOf(s) >= 0 ||
        catLbl.indexOf(s) >= 0;
      const matchType = state.filterType === 'all' || tx.type === state.filterType;
      const matchAcc  = !state.filterAccount || String(tx.accountId) === String(state.filterAccount);
      const matchCat  = !state.filterCategory || String(tx.categoryId) === String(state.filterCategory);
      const matchSt   = !state.filterStatus || tx.status === state.filterStatus;
      return matchSearch && matchType && matchAcc && matchCat && matchSt;
    });
  }

  function summary(list) {
    let income = 0, expense = 0;
    list.forEach(function (t) {
      if (t.type === 'income') income += Number(t.amount) || 0;
      else if (t.type === 'expense') expense += Math.abs(Number(t.amount) || 0);
    });
    return { income: income, expense: expense, result: income - expense };
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $page = $('<div class="fade-in"></div>');

    // ── Page header ──
    const $header = $(
      '<div class="page-header">' +
        '<h1>Lançamentos</h1>' +
        '<div class="page-header-actions" data-region="head-actions"></div>' +
      '</div>'
    );
    const $headRight = $header.find('[data-region=head-actions]');

    // period nav
    const $periodNav = window.periodNav({
      month: state.month + 1,
      year: state.year,
      onPrev: function () { window.shiftMonth(state, -1, false); loadTransactions(); },
      onNext: function () { window.shiftMonth(state, +1, false); loadTransactions(); },
      onChange: function (m, y) { window.App.PeriodService.set(m, y); state.month = m - 1; state.year = y; loadTransactions(); },
    });
    $headRight.append($periodNav);

    // Filter toggle
    $headRight.append(
      window.btn({
        variant: state.showFilters ? 'primary' : 'secondary',
        size: 'md', icon: 'filter',
        label: 'Filtros',
        attrs: 'data-act="toggle-filters"'
      })
    );

    // Importar
    $headRight.append(
      window.btn({
        variant: 'secondary', size: 'md', icon: 'download', label: 'Importar',
        attrs: 'data-act="import"'
      })
    );

    // Novo Lançamento
    $headRight.append(
      window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Novo Lançamento',
        attrs: 'data-act="new"'
      })
    );

    $page.append($header);

    // ── Summary cards ──
    const sum = summary(filteredTxs());
    const resultColor = sum.result >= 0 ? 'var(--income)' : 'var(--expense)';
    const $summary = $(
      '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:18px;">' +
        summaryCardHtml('arrowUp',   'RECEITAS',  fmt(sum.income),  'var(--income)') +
        summaryCardHtml('arrowDown', 'DESPESAS',  fmt(sum.expense), 'var(--expense)') +
        summaryCardHtml('trendingUp','RESULTADO', fmt(sum.result),  resultColor) +
      '</div>'
    );
    $page.append($summary);

    // ── Quick filter / search row ──
    const $quick = $(
      '<div style="display:flex;gap:10px;align-items:center;margin-bottom:14px;flex-wrap:wrap;">' +
        '<div style="position:relative;flex:1;max-width:320px;min-width:200px;">' +
          '<div style="position:absolute;left:10px;top:50%;transform:translateY(-50%);color:var(--text-muted);display:flex;">' +
            window.icon('search', 15) +
          '</div>' +
          '<input data-act="search" type="text" placeholder="Pesquisar lançamentos..." ' +
            'value="' + esc(state.search) + '" style="padding-left:32px;" />' +
        '</div>' +
        chipBtn('all', 'Todos') +
        chipBtn('income', 'Receitas') +
        chipBtn('expense', 'Despesas') +
        chipBtn('transfer', 'Transferências') +
      '</div>'
    );
    $page.append($quick);

    // ── Advanced filters ──
    if (state.showFilters) {
      $page.append(renderAdvancedFilters());
    }

    // ── Table / list — scrolls its own content inside the card. ──
    $page.append(renderList());

    // Footer: how many transactions are on display.
    if (!state.loading) {
      const txCount = filteredTxs().length;
      $page.append(
        '<div style="text-align:right;padding:12px 4px 0;font-size:12px;color:var(--text-muted);">' +
          esc(txCount + (txCount === 1 ? ' transação exibida' : ' transações exibidas')) +
        '</div>'
      );
    }

    $root.empty().append($page);

    // Re-apply the scroll position(s) captured at the start of loadTransactions() — only once
    // the real list is on screen (not the transient "Carregando…" placeholder).
    if (!state.loading) {
      if (state.pendingScrollTop != null) {
        $root.find('[data-region=list-card]').scrollTop(state.pendingScrollTop);
        state.pendingScrollTop = null;
      }
      if (state.pendingRootScrollTop != null) {
        $root.scrollTop(state.pendingRootScrollTop);
        state.pendingRootScrollTop = null;
      }
    }
  }

  function summaryCardHtml(iconName, label, value, color) {
    return (
      '<div class="card" style="padding:14px 18px;display:flex;align-items:center;gap:12px;">' +
        '<span style="color:' + color + ';display:flex;">' + window.icon(iconName, 20) + '</span>' +
        '<div>' +
          '<p style="font-size:11px;color:var(--text-muted);font-weight:600;letter-spacing:0.04em;">' +
            esc(label) +
          '</p>' +
          '<p style="font-size:18px;font-weight:800;color:' + color + ';margin-top:2px;">' +
            esc(value) +
          '</p>' +
        '</div>' +
      '</div>'
    );
  }

  function chipBtn(val, label) {
    const active = state.filterType === val;
    const style =
      'padding:6px 14px;border-radius:var(--radius-sm);font-size:13px;font-weight:500;' +
      'border:1px solid var(--border);cursor:pointer;transition:all var(--transition);' +
      (active
        ? 'background:var(--accent);color:#fff;'
        : 'background:transparent;color:var(--text-secondary);');
    return '<button type="button" data-act="set-type" data-type="' + esc(val) + '" ' +
      'style="' + style + '">' + esc(label) + '</button>';
  }

  function renderAdvancedFilters() {
    const accs = accountsList();
    // excludeRoots: true — raiz nunca casa contra tx.categoryId (só subcategoria é atribuível a um
    // lançamento), então oferecê-la no filtro só rende opção morta que nunca filtra nada.
    const catFieldHtml = window.categoryPickerHtml({
      items: flatCategories(null, true, state.filterCategory),
      selectedId: state.filterCategory,
      selectId: 'tx-filter-category',
      selectAttrs: ' data-act="filter-category"',
      placeholder: 'Todas',
      alwaysPlaceholder: true,
    });

    const accOpts = '<option value="">Todas</option>' + accs.map(function (a) {
      const sel = String(a.id) === String(state.filterAccount) ? ' selected' : '';
      return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
    }).join('');

    const stOpts = ['', 'confirmed', 'pending', 'scheduled', 'planed'].map(function (st) {
      const lbl = st === '' ? 'Todos' : (STATUS_LABEL[st] || st);
      const sel = st === state.filterStatus ? ' selected' : '';
      return '<option value="' + esc(st) + '"' + sel + '>' + esc(lbl) + '</option>';
    }).join('');

    const hasAny = !!(state.filterAccount || state.filterCategory || state.filterStatus);

    return $(
      '<div class="card" style="padding:16px;margin-bottom:14px;">' +
        '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;">' +
          '<div class="form-group">' +
            '<label class="form-label">Conta</label>' +
            '<select data-act="filter-account">' + accOpts + '</select>' +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label">Categoria</label>' +
            catFieldHtml +
          '</div>' +
          '<div class="form-group">' +
            '<label class="form-label">Status</label>' +
            '<select data-act="filter-status">' + stOpts + '</select>' +
          '</div>' +
        '</div>' +
        (hasAny
          ? '<div style="display:flex;justify-content:flex-end;margin-top:10px;">' +
              '<button type="button" data-act="clear-filters" class="btn btn-ghost btn-sm">' +
                window.icon('x', 13) + '<span>Limpar filtros</span>' +
              '</button>' +
            '</div>'
          : ''
        ) +
      '</div>'
    );
  }

  // Statement-style card: data | conta (coluna fixa) | categoria (coluna fixa) | descrição | badge | valor | ações.
  function renderList() {
    const list = filteredTxs();

    const $card = $(
      '<div class="card" data-region="list-card" style="padding:0;overflow-y:auto;' +
        'max-height:calc(100vh - 300px);min-height:200px;"></div>'
    );

    if (state.loading) {
      $card.append(window.emptyState({ icon: 'list', title: 'Carregando…' }));
      return $card;
    }
    if (list.length === 0) {
      $card.append(window.emptyState({
        icon: 'list',
        title: 'Nenhum lançamento encontrado',
        desc: 'Ajuste os filtros ou crie um novo lançamento.'
      }));
      return $card;
    }

    const catMap = categoryById();
    const accs = window.App.CacheStore.accounts();
    const accMap = {};
    accs.forEach(function (a) { accMap[String(a.id)] = a; });

    // Fixed category column: width of the widest possible category label so
    // every description starts at the same x, regardless of each row's category.
    const catLens = window.flatCategories().map(function (c) { return c.label.length; });
    const catColCh = (catLens.length ? Math.max.apply(null, catLens) : 12) + 1;

    // Fixed account column, same idea (widest account name).
    const accLens = accs.map(function (a) { return (a.name || '').length; });
    const accColCh = (accLens.length ? Math.max.apply(null, accLens) : 10) + 1;

    // Sort by date desc.
    const sorted = list.slice().sort(function (a, b) {
      return String(b.date).localeCompare(String(a.date));
    });

    sorted.forEach(function (tx, i) {
      const isLast = i === sorted.length - 1;
      const cat = catMap[tx.categoryId];
      const catLbl = cat ? categoryLabel(cat) : '';
      const acc = accMap[String(tx.accountId)];
      const accName = acc ? acc.name : '—';
      const amt = Number(tx.amount) || 0;
      const amtColor =
        tx.type === 'income' ? 'var(--income)' :
        tx.type === 'transfer' ? 'var(--text-secondary)' :
        'var(--expense)';
      const stKey = tx.status || 'confirmed';

      const rowStyle =
        'display:flex;align-items:center;gap:16px;padding:11px 20px;' +
        (isLast ? '' : 'border-bottom:1px solid var(--border-light);') +
        'transition:background var(--transition);';

      const descStyle =
        'flex:1;font-size:13px;font-weight:500;color:var(--text-primary);' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-transform:uppercase;';

      const catStyle =
        'flex:0 0 ' + catColCh + 'ch;width:' + catColCh + 'ch;' +
        'font-size:12px;color:var(--text-muted);' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

      const accStyle =
        'flex:0 0 ' + accColCh + 'ch;width:' + accColCh + 'ch;' +
        'font-size:12px;color:var(--text-muted);text-align:left;' +
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

      // Row actions; mark-paid only for non-confirmed rows.
      const markPaidHtml = stKey !== 'confirmed'
        ? '<button type="button" class="icon-btn" title="Confirmar" ' +
            'data-act="mark-paid" data-id="' + esc(tx.id) + '" ' +
            'style="width:28px;height:28px;color:var(--income);">' +
            window.icon('check', 14) +
          '</button>'
        : '';
      // Linha de cartões é derivada — edita-se cada compra no extrato do cartão.
      const actionsHtml = tx.invoice ? '' :
        markPaidHtml +
        '<button type="button" class="icon-btn" title="Editar" ' +
          'data-act="edit" data-id="' + esc(tx.id) + '" style="width:28px;height:28px;">' +
          window.icon('edit', 14) +
        '</button>' +
        '<button type="button" class="icon-btn" title="Excluir" ' +
          'data-act="trash" data-id="' + esc(tx.id) + '" style="width:28px;height:28px;color:var(--expense);">' +
          window.icon('trash', 14) +
        '</button>';

      // O link é um <a href> de verdade: o router é hash-based, então a navegação não precisa de
      // handler (e o deep-link fica copiável/abrível em nova aba).
      const descText = window.Domain.Transaction.describe(tx) || '—';
      const descHtml = tx.invoice
        ? '<a href="#/credit-cards" style="' + descStyle +
            'color:var(--accent);text-decoration:none;">' +
            esc(descText) +
          '</a>'
        : '<span style="' + descStyle + '">' + esc(descText) + '</span>';

      $card.append(
        '<div class="stm-row" data-row="tx" data-id="' + esc(tx.id) + '" style="' + rowStyle + '">' +
          '<span style="font-size:12px;color:var(--text-muted);min-width:24px;text-align:left;">' +
            esc(i + 1) +
          '</span>' +
          '<span style="font-size:12px;color:var(--text-muted);min-width:56px;">' +
            esc(fmtDate(tx.date)) +
          '</span>' +
          '<span style="' + accStyle + '">' + esc(accName) + '</span>' +
          '<span style="' + catStyle + '">' + esc(catLbl) + '</span>' +
          window.tagFlagHtml(tx.tagIds) +
          descHtml +
          '<span class="badge badge-' + esc(STATUS_BADGE[stKey] || 'muted') + '" ' +
          'style="flex-shrink:0;">' + esc(STATUS_LABEL[stKey] || stKey) + '</span>' +
          '<span style="font-size:13px;font-weight:700;color:' + amtColor + ';min-width:100px;text-align:right;">' +
            esc(fmt(amt)) +
          '</span>' +
          '<div style="display:flex;align-items:center;justify-content:flex-end;gap:2px;width:40px;flex-shrink:0;">' + actionsHtml + '</div>' +
        '</div>'
      );
    });

    return $card;
  }

  // ── Event delegation ──────────────────────────────────────
  function bindRoot($root) {
    $root.on('click.tx', '[data-act=new]', function () { openFormModal(null); });
    $root.on('click.tx', '[data-act=import]', function () {
      window.ImportStatementApi.open({ onImported: function () { return loadTransactions(); } });
    });

    $root.on('click.tx', '[data-act=toggle-filters]', function () {
      state.showFilters = !state.showFilters;
      render();
    });

    $root.on('click.tx', '[data-act=set-type]', function () {
      const t = $(this).attr('data-type');
      if (t && t !== state.filterType) {
        state.filterType = t;
        render();
      }
    });

    $root.on('input.tx', '[data-act=search]', function () {
      state.search = $(this).val() || '';
      // Preserve focus by refocusing the search input after render.
      const caretPos = this.selectionStart;
      render();
      const $input = state.$root.find('[data-act=search]');
      $input.trigger('focus');
      try { $input[0].setSelectionRange(caretPos, caretPos); } catch (e) { /* noop */ }
    });

    $root.on('change.tx', '[data-act=filter-account]', function () {
      state.filterAccount = $(this).val() || '';
      render();
    });
    $root.on('change.tx', '[data-act=filter-category]', function () {
      state.filterCategory = $(this).val() || '';
      render();
    });
    $root.on('change.tx', '[data-act=filter-status]', function () {
      state.filterStatus = $(this).val() || '';
      render();
    });
    $root.on('click.tx', '[data-act=clear-filters]', function () {
      state.filterAccount = '';
      state.filterCategory = '';
      state.filterStatus = '';
      render();
    });

    $root.on('click.tx', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const tx = findTx($(this).attr('data-id'));
      if (tx) openFormModal(tx);
    });
    $root.on('click.tx', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const tx = findTx($(this).attr('data-id'));
      if (tx) window.transactionActions.openDeleteModal(tx, { list: state.transactions, onDone: loadTransactions });
    });
    $root.on('click.tx', '[data-act=mark-paid]', function (e) {
      e.stopPropagation();
      const tx = findTx($(this).attr('data-id'));
      if (tx) window.transactionActions.markPaid(tx, { onDone: loadTransactions });
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['transactions'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      render();
      loadTransactions();
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.tx');
      }
      state = null;
    }
  };
})();

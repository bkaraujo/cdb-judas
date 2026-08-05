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

    function buildCatOptions(type, selectedId) {
      const nat = natureForType(type);
      const cats = flatCategories(nat, true, selectedId);
      if (!cats.length) return '<option value="">Nenhuma categoria disponível</option>';
      return cats.map(function (c) {
        const sel = String(c.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.label) + '</option>';
      }).join('');
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
          '<select id="' + ids.category + '" name="categoryId" data-region="category-select">' +
            buildCatOptions(type, initial.categoryId) +
          '</select>' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.amount + '">Valor (R$)</label>' +
          '<input id="' + ids.amount + '" name="amount" type="text" inputmode="numeric" ' +
            'placeholder="0,00" value="' + esc(initial.amount) + '" />' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label" for="' + ids.status + '">Status</label>' +
          '<select id="' + ids.status + '" name="status">' + statusOpts + '</select>' +
        '</div>' +
        '<div class="form-group">' +
          '<label class="form-label">Tags</label>' +
          window.tagsDropdownHtml(initial.tagIds, 'tx') +
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
      const rule = window.Domain.ImportRuleMatcher.match(value, window.App.ImportRuleService.listCached());
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
      openCategoryCreateModal(nature, function (created) {
        const $sel = $form.find('select[name=categoryId]');
        // Drop the "Nenhuma categoria disponível" placeholder, if present.
        $sel.find('option').each(function () { if (!this.value) $(this).remove(); });
        if (!$sel.find('option[value="' + esc(created.id) + '"]').length) {
          $sel.append('<option value="' + esc(created.id) + '">' +
            esc(quickCategoryLabel(created)) + '</option>');
        }
        $sel.val(String(created.id));
        initial.categoryId = String(created.id);
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

  // ── Modal: quick-create category (nested, from the tx form) ─
  // Label for a freshly-created category not yet in the cache (SSE lag).
  function quickCategoryLabel(cat) {
    const name = cat.name || cat.description || '';
    if (cat.parentId) {
      const parent = window.categoryById()[cat.parentId];
      if (parent) return parent.name + ' / ' + name;
    }
    return name;
  }

  // Nature is fixed by the originating transaction type. On success the created
  // category (with id) is handed to `onCreated` so the caller can select it.
  function openCategoryCreateModal(nature, onCreated) {
    const uniq = Date.now();
    const nameId = 'qcat-name-' + uniq;
    const parentSelId = 'qcat-parent-' + uniq;
    const natureLabel = nature === 'INCOME' ? 'Receita' : 'Despesa';

    const roots = window.App.CacheStore.categories().filter(function (c) {
      return String(c.nature || '').toUpperCase() === nature && !c.parentId;
    }).slice().sort(window.sortByName);

    const parentOpts = '<option value="">— Nenhuma (categoria raiz) —</option>' +
      roots.map(function (p) {
        return '<option value="' + esc(p.id) + '">' + esc(p.name) + '</option>';
      }).join('');

    const bodyHtml =
      '<form data-form="qcat" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label">Tipo</label>' +
            '<input type="text" value="' + esc(natureLabel) + '" disabled />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + nameId + '">Nome</label>' +
            '<input id="' + nameId + '" name="name" type="text" required ' +
              'placeholder="Nome da categoria" />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + parentSelId + '">Categoria Pai (opcional)</label>' +
            '<select id="' + parentSelId + '" name="parentId">' + parentOpts + '</select>' +
          '</div>' +
        '</div>' +
      '</form>';

    const m = window.modal({
      title: 'Nova Categoria',
      body: bodyHtml,
      footer: window.saveCancelFooter({ saveAttrs: 'data-act="qcat-save" type="submit"' }),
    });
    m.open();

    const $form = m.$body.find('form[data-form=qcat]');
    $form.find('input[name=name]').trigger('focus');

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($form.find('input[name=name]').val() || '').trim();
      if (!name) { $form.find('input[name=name]').trigger('focus'); return; }
      const parentId = $form.find('select[name=parentId]').val() || null;

      const $btn = m.$el.find('[data-act=qcat-save]').prop('disabled', true);
      window.App.CategoryService.create({
        name: name,
        nature: nature,
        parentId: parentId,
      }).then(function (created) {
        m.close();
        window.toast('Categoria criada', 'success');
        if (onCreated) onCreated(created);
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao criar categoria', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=qcat-save]', submit);
  }
})();

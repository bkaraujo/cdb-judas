/* pages/transactions/import-statement.js — Diálogos de importação de extrato/fatura em PDF.
 *
 * Modal único que cobre dois fluxos, detectados automaticamente pelo backend a partir do PDF:
 *   - CREDIT_CARD_INVOICE: fatura de cartão (Santander/BTG) → escolhe cartão de destino.
 *   - BANK_STATEMENT: extrato de conta corrente (BTG) → escolhe conta + estado (novo/concilia/duplicado).
 *
 * Uso:
 *   window.importStatementModal({ onImported: function () { return loadTransactions(); } });
 *
 * O backend faz preview (TransactionService.importPreview) e confirmação
 * (TransactionService.importConfirm). PDFs protegidos pedem senha sob demanda
 * (códigos PASSWORD_REQUIRED / WRONG_PASSWORD).
 */
(function () {
  window.importStatementModal = function (opts) {
    opts = opts || {};
    const onImported = typeof opts.onImported === 'function'
      ? opts.onImported
      : function () { return null; };

    const uniq = Date.now();
    const fileId = 'import-file-' + uniq;
    const pwdId = 'import-pwd-' + uniq;
    const cardSelectId = 'import-card-' + uniq;

    const bodyHtml =
      '<div class="form-group">' +
        '<label class="form-label" for="' + fileId + '">Selecionar fatura (PDF)</label>' +
        '<input id="' + fileId + '" type="file" accept=".pdf,application/pdf" ' +
          'style="padding: 24px; border: 2px dashed var(--border); border-radius: var(--radius); ' +
          'background: var(--bg-hover); color: var(--text-primary); cursor: pointer; text-align: center; width: 100%;" />' +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:8px;">' +
          'Envie o PDF original: fatura do cartão (Santander ou BTG) ou extrato da conta corrente (BTG). ' +
          'O tipo é detectado automaticamente.' +
        '</p>' +
        '<button type="button" data-act="toggle-password" ' +
          'style="margin-top:6px;background:none;border:none;color:var(--accent);cursor:pointer;font-size:12px;padding:0;">' +
          'PDF protegido? Informar senha' +
        '</button>' +
      '</div>' +
      '<div class="form-group" data-region="password-block" style="display:none;">' +
        '<label class="form-label" for="' + pwdId + '">Senha do PDF</label>' +
        '<input id="' + pwdId + '" type="password" autocomplete="off" placeholder="Senha da fatura" />' +
        '<p data-region="password-hint" style="font-size:12px;color:var(--expense);margin-top:6px;display:none;"></p>' +
      '</div>';

    const $cancel = window.btn({
      variant: 'ghost', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $import = window.btn({
      variant: 'primary', size: 'md', icon: 'download', label: 'Importar',
      attrs: 'data-act="do-import" type="button"'
    });
    const $footer = window.modalFooter([$cancel, $import]);

    const m = window.modal({
      title: 'Importar Fatura ou Extrato',
      body: bodyHtml,
      footer: $footer,
      persistent: true
    });
    m.open();
    m.$el.find('.modal-box').css({ width: '70vw', 'min-width': '560px', 'max-height': '90vh', display: 'flex', 'flex-direction': 'column', overflow: 'hidden' });
    m.$body.css({ flex: '1', 'overflow-y': 'scroll', 'min-height': '0', display: 'flex', 'flex-direction': 'column' });

    let selectedFile = null;
    let cardCandidates = [];
    let selectedAccountId = null;
    let previewData = null;
    let statementData = null;
    let lastPassword = null;
    let sortCol = null;
    let sortAsc = true;
    const $pwdBlock = m.$el.find('[data-region=password-block]');
    const $pwd = m.$el.find('#' + pwdId);
    const $pwdHint = m.$el.find('[data-region=password-hint]');

    m.$el.find('#' + fileId).on('change', function (e) {
      selectedFile = (e.target.files && e.target.files[0]) || null;
    });

    function revealPassword(message) {
      $pwdBlock.show();
      if (message) $pwdHint.text(message).show();
      $pwd.trigger('focus');
    }

    function fmtIsoDate(iso) {
      const mm = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(iso || ''));
      return mm ? (mm[3] + '/' + mm[2] + '/' + mm[1]) : (iso || '');
    }

    function statusTag(status) {
      const scheduled = status === 'scheduled';
      const label = scheduled ? 'Agendado' : 'Confirmado';
      const color = scheduled ? 'var(--text-muted)' : 'var(--income)';
      return '<span style="font-size:11px;color:' + color + ';">' + esc(label) + '</span>';
    }

    function importCategories(keepId) {
      const expense = flatCategories('EXPENSE', true, keepId);
      return expense.length ? expense : flatCategories(null, true, keepId);
    }

    // Import category set for a given movement type (statement rows can be income or expense).
    function importCategoriesFor(type, keepId) {
      const nature = type === 'income' ? 'INCOME' : 'EXPENSE';
      const byNature = flatCategories(nature, true, keepId);
      return byNature.length ? byNature : flatCategories(null, true, keepId);
    }

    function categorySelectHtml(selectedId, idx, groupId, locked) {
      const groupAttr = ' data-group-id="' + esc(groupId || '') + '"';
      const cats = importCategories(selectedId);
      if (!cats.length) {
        return '<select data-row-category data-idx="' + idx + '"' + groupAttr + ' disabled>' +
          '<option value="">Sem categorias</option></select>';
      }
      const options = cats.map(function (c) {
        const sel = String(c.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.label) + '</option>';
      }).join('');
      const lockedAttrs = locked ? ' disabled title="Segue a categoria da 1ª parcela do grupo"' : '';
      return '<select data-row-category data-idx="' + idx + '"' + groupAttr + lockedAttrs + ' ' +
        'style="width:auto;font-size:12px;padding:4px 6px;' + (locked ? 'opacity:0.6;' : '') + '">' + options + '</select>';
    }

    // Mesma convenção de create-edit.js: sem valor da regra, pré-seleciona o centro "Variável"
    // (o backend faz o mesmo fallback quando a linha chega sem costCenterId no confirm).
    function costCenterSelectHtml(selectedId, idx) {
      const ccs = window.App.CacheStore.costCenters();
      if (!ccs.length) {
        return '<select data-row-costcenter data-idx="' + idx + '" disabled>' +
          '<option value="">Sem centros</option></select>';
      }
      const variavel = ccs.filter(function (c) { return /vari/i.test(c.description || c.name || ''); })[0];
      const target = selectedId || (variavel && variavel.id) || '';
      const options = ccs.map(function (c) {
        const label = c.description || c.name || '';
        const sel = String(c.id) === String(target) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(label) + '</option>';
      }).join('');
      return '<select data-row-costcenter data-idx="' + idx + '" ' +
        'style="width:auto;font-size:12px;padding:4px 6px;">' + options + '</select>';
    }

    // Regra de nomenclatura: casa a descrição crua de cada linha contra o cache de regras e, ao
    // bater, sobrescreve descrição/categoria/centro de custo (nunca conta — extrato já tem conta
    // fixa por importação, fatura casa por cartão) antes da linha ser renderizada. Roda antes de
    // alignGroupFields() na fatura, pra a propagação de parcelas herdar o resultado da 1ª parcela.
    function applyImportRules(rows) {
      const rules = window.App.ImportRuleService.listCached();
      if (!rules.length) return;
      rows.forEach(function (row) {
        const rule = window.Domain.ImportRuleMatcher.match(row.description, rules);
        if (!rule) return;
        row.description = rule.name;
        if (rule.categoryId) row.categoryId = rule.categoryId;
        if (rule.costCenterId) row.costCenterId = rule.costCenterId;
      });
    }

    function cardOptionShort(card) {
      return card.last4 ? (card.name + ' — •••• ' + card.last4) : card.name;
    }

    // Per-row card selector: o cartão é definido por transação. Só os cartões presentes na fatura são
    // oferecidos. Linha casada por last4 vem pré-selecionada; sem match, um placeholder exige a escolha.
    function cardSelectHtml(selectedId, idx) {
      if (!cardCandidates.length) return '—';
      const hasSel = cardCandidates.some(function (c) { return String(c.id) === String(selectedId); });
      const placeholder = hasSel ? '' : '<option value="" selected>Selecione</option>';
      const options = placeholder + cardCandidates.map(function (c) {
        const sel = String(c.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(cardOptionShort(c)) + '</option>';
      }).join('');
      return '<select data-row-card data-idx="' + idx + '" ' +
        'style="width:auto;font-size:12px;padding:4px 6px;">' + options + '</select>';
    }

    function previewRowHtml(row, idx) {
      const cardSelected = row.cardId !== undefined ? row.cardId : row.suggestedCardId;
      const hasParcela = row.installmentNumber != null && row.installmentTotal != null;
      const parcela = hasParcela ? esc(row.installmentNumber + '/' + row.installmentTotal) : '—';
      const dup = !!row.duplicate;
      const dupTag = dup
        ? ' <span style="color:var(--text-muted);font-size:11px;font-style:italic;">já importado</span>'
        : '';
      const checked = row.checked !== undefined ? (row.checked ? 'checked' : '') : (dup ? '' : 'checked');
      const groupLocked = !!(row.groupId && row.installmentNumber != null && row.installmentNumber !== 1);
      const descLockedAttrs = groupLocked ? ' readonly title="Segue a descrição da 1ª parcela do grupo"' : '';
      return (
        '<tr style="border-top:1px solid var(--border);' + (dup ? 'opacity:0.6;' : '') + '">' +
          '<td style="padding:8px 10px;text-align:center;">' +
            '<input type="checkbox" data-row-include data-idx="' + idx + '" ' + checked + ' ' +
              'style="width:16px;height:16px;cursor:pointer;" />' +
          '</td>' +
          '<td style="padding:8px 10px;width:90px;white-space:nowrap;color:var(--text-secondary);">' + esc(fmtIsoDate(row.date)) + '</td>' +
          '<td style="padding:8px 10px;">' + categorySelectHtml(row.categoryId, idx, row.groupId, groupLocked) + '</td>' +
          '<td style="padding:8px 10px;">' + costCenterSelectHtml(row.costCenterId, idx) + '</td>' +
          '<td style="padding:8px 10px;text-align:center;color:var(--text-secondary);">' + parcela + '</td>' +
          '<td style="padding:8px 10px;">' +
            '<input type="text" data-row-description data-idx="' + idx + '" data-group-id="' + esc(row.groupId || '') + '" value="' + esc(row.description) + '"' + descLockedAttrs + ' ' +
              'style="width:100%;font-size:12px;padding:4px 6px;border:1px solid transparent;background:transparent;color:inherit;outline:none;text-transform:uppercase;' + (groupLocked ? 'opacity:0.6;' : '') + '" ' +
              'onfocus="this.style.border=\'1px solid var(--border)\';this.style.background=\'var(--bg-card)\'" ' +
              'onblur="this.style.border=\'1px solid transparent\';this.style.background=\'transparent\'" />' +
            dupTag +
          '</td>' +
          '<td style="padding:8px 10px;">' + cardSelectHtml(cardSelected, idx) + '</td>' +
          '<td style="padding:8px 10px;text-align:center;white-space:nowrap;">' + statusTag(row.status) + '</td>' +
          '<td style="padding:8px 10px;text-align:right;font-weight:700;white-space:nowrap;">' + esc(fmt(row.amount)) + '</td>' +
        '</tr>'
      );
    }

    function showPreview(preview) {
      previewData = preview;
      const issuer = (preview && preview.issuer) || 'UNKNOWN';
      const label = issuer === 'SANTANDER' ? 'Santander'
                  : issuer === 'BTG' ? 'BTG Pactual'
                  : issuer;
      const last4s = (preview && preview.last4s) || [];
      const rows = (preview && preview.rows) || [];
      alignGroupFields(rows);

      const candidateCards = (preview && preview.candidateCards) || [];
      cardCandidates = candidateCards;

      const cardsLine = last4s.length
        ? '<p style="font-size:12px;color:var(--text-muted);margin-top:2px;">' +
            'Cartões: ' + esc(last4s.map(function (l) { return '•••• ' + l; }).join('  ')) +
          '</p>'
        : '';

      // Sem cartão padrão: o cartão é escolhido por linha. Se nenhum cartão da fatura está cadastrado,
      // não há como atribuir os lançamentos — avisa o usuário.
      const noCardsWarning = (!candidateCards.length && rows.length)
        ? '<p style="font-size:12px;color:var(--expense);margin-top:12px;">' +
            'Nenhum cartão cadastrado corresponde aos 4 últimos dígitos desta fatura. ' +
            'Cadastre o cartão para importar os lançamentos.' +
          '</p>'
        : '';

      function sortIcon(col) {
        if (sortCol !== col) return '';
        return sortAsc ? ' ↑' : ' ↓';
      }

      const tableRows = rows.map(previewRowHtml).join('');
      const tableHtml = rows.length
        ? '<div style="flex:1;overflow-y:scroll;min-height:0;border:1px solid var(--border);border-radius:var(--radius-sm);margin-top:12px;">' +
            '<table style="width:100%;border-collapse:collapse;font-size:13px;">' +
              '<thead><tr style="position:sticky;top:0;background:var(--bg-hover);">' +
                '<th style="padding:8px 10px;width:34px;text-align:center;">' +
                  '<input type="checkbox" data-act="select-all" checked style="width:16px;height:16px;cursor:pointer;" />' +
                '</th>' +
                '<th data-sort="date" style="padding:8px 10px;width:90px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Data' + sortIcon('date') + '</th>' +
                '<th data-sort="categoryId" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Categoria' + sortIcon('categoryId') + '</th>' +
                '<th data-sort="costCenterId" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Centro de Custo' + sortIcon('costCenterId') + '</th>' +
                '<th data-sort="installmentNumber" style="padding:8px 10px;text-align:center;font-size:11px;color:var(--text-muted);cursor:pointer;">Parcela' + sortIcon('installmentNumber') + '</th>' +
                '<th data-sort="description" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Descrição' + sortIcon('description') + '</th>' +
                '<th data-sort="suggestedCardId" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Cartão' + sortIcon('suggestedCardId') + '</th>' +
                '<th data-sort="status" style="padding:8px 10px;text-align:center;font-size:11px;color:var(--text-muted);cursor:pointer;">Status' + sortIcon('status') + '</th>' +
                '<th data-sort="amount" style="padding:8px 10px;text-align:right;font-size:11px;color:var(--text-muted);cursor:pointer;">Valor' + sortIcon('amount') + '</th>' +
              '</tr></thead>' +
              '<tbody>' + tableRows + '</tbody>' +
            '</table>' +
          '</div>'
        : '<p style="font-size:13px;color:var(--text-muted);margin-top:12px;">Nenhum lançamento encontrado na fatura.</p>';

      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + window.icon('check', 22) + '</span>' +
          '<div>' +
            '<p style="font-size:13px;color:var(--text-muted);">Banco detectado</p>' +
            '<p style="font-size:18px;font-weight:800;">' + esc(label) + '</p>' +
            cardsLine +
          '</div>' +
        '</div>' +
        noCardsWarning +
        tableHtml +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:10px;">' +
          esc(rows.length + ' lançamento(s) encontrado(s). Revise, ajuste as categorias e o cartão de cada linha e confirme a importação.') +
        '</p>'
      );

      // Swap the upload action for a confirm action (only when there are rows to import).
      m.$el.find('[data-act=do-import]').hide();
      const $foot = m.$el.find('.modal-footer');
      $foot.find('[data-act=do-confirm]').remove();
      if (rows.length) {
        $foot.append(window.btn({
          variant: 'primary', size: 'md', icon: 'check', label: 'Confirmar importação',
          attrs: 'data-act="do-confirm" type="button"'
        }));
      }
    }

    function confirmImport() {
      const preview = previewData;
      if (!preview) return;

      const rows = [];
      let missingCard = false;
      m.$el.find('[data-row-include]').each(function () {
        if (!this.checked) return;
        const idx = Number($(this).attr('data-idx'));
        const src = preview.rows && preview.rows[idx];
        if (!src) return;
        const $cat = m.$el.find('[data-row-category][data-idx="' + idx + '"]');
        const $costCenter = m.$el.find('[data-row-costcenter][data-idx="' + idx + '"]');
        const $desc = m.$el.find('[data-row-description][data-idx="' + idx + '"]');
        const $card = m.$el.find('[data-row-card][data-idx="' + idx + '"]');
        const categoryId = ($cat.val() || src.categoryId) || null;
        const costCenterId = ($costCenter.val() || src.costCenterId) || null;
        const description = ($desc.val() || src.description || '').trim();
        // O cartão é definido por transação — cada linha incluída precisa de um cartão.
        const cardId = $card.length ? ($card.val() || null) : null;
        if (!cardId) { missingCard = true; }
        rows.push({
          description: description,
          amount: src.amount,
          date: src.date,
          originalDate: src.originalDate,
          installmentNumber: src.installmentNumber,
          installmentTotal: src.installmentTotal,
          categoryId: categoryId,
          costCenterId: costCenterId,
          cardId: cardId,
        });
      });

      if (!rows.length) { window.toast('Selecione ao menos um lançamento', 'error'); return; }
      if (missingCard) { window.toast('Selecione o cartão de cada lançamento', 'error'); return; }

      const $btn = m.$el.find('[data-act=do-confirm]').prop('disabled', true);
      window.App.TransactionService.importConfirm({ type: 'CREDIT_CARD_INVOICE', rows: rows })
        .then(function (res) {
          const created = (res && res.created) || 0;
          const skipped = (res && res.skipped) || 0;
          showConfirmSummary(created, skipped);
          return onImported();
        })
        .catch(function (err) {
          $btn.prop('disabled', false);
          window.toast((err && err.message) || 'Falha ao confirmar a importação', 'error');
        });
    }

    function showConfirmSummary(created, skipped, reconciled) {
      const parts = [created + ' criados'];
      if (reconciled) parts.push(reconciled + ' conciliados');
      parts.push(skipped + ' ignorados');
      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + window.icon('check', 22) + '</span>' +
          '<div>' +
            '<p style="font-size:18px;font-weight:800;">Importação concluída</p>' +
            '<p style="font-size:13px;color:var(--text-muted);margin-top:2px;">' +
              esc(parts.join(', ')) +
            '</p>' +
          '</div>' +
        '</div>'
      );
      const $foot = m.$el.find('.modal-footer');
      $foot.empty().append(window.btn({
        variant: 'primary', size: 'md', label: 'Fechar',
        attrs: 'data-modal-close="1" type="button"'
      }));
    }

    // ── Bank statement (extrato) preview ───────────────────────
    function routePreview(preview) {
      applyImportRules((preview && preview.rows) || []);
      if (preview && preview.documentType === 'BANK_STATEMENT') {
        showStatementPreview(preview);
      } else {
        showPreview(preview);
      }
    }

    function statementCategorySelectHtml(selectedId, idx, type) {
      const cats = importCategoriesFor(type, selectedId);
      if (!cats.length) {
        return '<select data-row-category data-idx="' + idx + '" disabled>' +
          '<option value="">Sem categorias</option></select>';
      }
      const options = cats.map(function (c) {
        const sel = String(c.id) === String(selectedId) ? ' selected' : '';
        return '<option value="' + esc(c.id) + '"' + sel + '>' + esc(c.label) + '</option>';
      }).join('');
      return '<select data-row-category data-idx="' + idx + '" ' +
        'style="width:auto;font-size:12px;padding:4px 6px;">' + options + '</select>';
    }

    function stateBadge(st) {
      if (st === 'DUPLICATE') {
        return '<span class="badge badge-muted">já importado</span>';
      }
      if (st === 'RECONCILE') {
        return '<span class="badge badge-warning">concilia</span>';
      }
      return '<span class="badge badge-income">novo</span>';
    }

    function statementRowHtml(row, idx) {
      const st = row.state;
      const dup = st === 'DUPLICATE';
      const checked = row.checked !== undefined ? (row.checked ? 'checked' : '') : (dup ? '' : 'checked');
      const amt = Number(row.amount) || 0;
      const amtColor = amt < 0 ? 'var(--expense)' : 'var(--income)';
      const note = (st === 'RECONCILE' && row.reconcileDescription)
        ? ' <span style="color:var(--text-muted);font-size:11px;font-style:italic;">↔ ' + esc(row.reconcileDescription) + '</span>'
        : '';
      return (
        '<tr style="border-top:1px solid var(--border);' + (dup ? 'opacity:0.6;' : '') + '">' +
          '<td style="padding:8px 10px;text-align:center;">' +
            '<input type="checkbox" data-row-include data-idx="' + idx + '" ' + checked + ' ' +
              'style="width:16px;height:16px;cursor:pointer;" />' +
          '</td>' +
          '<td style="padding:8px 10px;width:90px;white-space:nowrap;color:var(--text-secondary);">' + esc(fmtIsoDate(row.date)) + '</td>' +
          '<td style="padding:8px 10px;">' + statementCategorySelectHtml(row.categoryId, idx, row.type) + '</td>' +
          '<td style="padding:8px 10px;">' + costCenterSelectHtml(row.costCenterId, idx) + '</td>' +
          '<td style="padding:8px 10px;">' +
            '<input type="text" data-row-description data-idx="' + idx + '" value="' + esc(row.description) + '" ' +
              'style="width:100%;font-size:12px;padding:4px 6px;border:1px solid transparent;background:transparent;color:inherit;outline:none;text-transform:uppercase;" ' +
              'onfocus="this.style.border=\'1px solid var(--border)\';this.style.background=\'var(--bg-card)\'" ' +
              'onblur="this.style.border=\'1px solid transparent\';this.style.background=\'transparent\'" />' +
            note +
          '</td>' +
          '<td style="padding:8px 10px;text-align:center;white-space:nowrap;">' + stateBadge(st) + '</td>' +
          '<td style="padding:8px 10px;text-align:right;font-weight:700;white-space:nowrap;color:' + amtColor + ';">' + esc(fmt(amt)) + '</td>' +
        '</tr>'
      );
    }

    function showStatementPreview(preview) {
      statementData = preview;
      const issuer = (preview && preview.issuer) || 'UNKNOWN';
      const issuerLabel = issuer === 'SANTANDER' ? 'Santander'
                        : issuer === 'BTG' ? 'BTG Pactual'
                        : issuer;
      const accounts = (preview && preview.candidateAccounts) || [];
      // Backend só resolve selectedAccountId quando inequívoco (accountId explícito ou única conta ativa).
      // Sem isso, não há dado algum ligando o issuer detectado a uma conta — não adivinhar, forçar escolha.
      selectedAccountId = (preview && preview.selectedAccountId) || selectedAccountId || null;
      const rows = (preview && preview.rows) || [];

      const accOptions = (selectedAccountId ? '' : '<option value="" selected>Selecione a conta</option>') +
        accounts.map(function (a) {
          const sel = String(a.id) === String(selectedAccountId) ? ' selected' : '';
          return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
        }).join('');
      const accSelectorHtml = accounts.length
        ? '<div class="form-group" style="margin-top:12px;">' +
            '<label class="form-label" for="' + cardSelectId + '">Conta de destino</label>' +
            '<select id="' + cardSelectId + '" data-region="account-select">' + accOptions + '</select>' +
          '</div>'
        : '<p style="font-size:12px;color:var(--expense);margin-top:12px;">Nenhuma conta disponível para importação.</p>';

      function sortIcon(col) {
        if (sortCol !== col) return '';
        return sortAsc ? ' ↑' : ' ↓';
      }

      const tableRows = rows.map(statementRowHtml).join('');
      const tableHtml = rows.length
        ? '<div style="flex:1;overflow-y:scroll;min-height:0;border:1px solid var(--border);border-radius:var(--radius-sm);margin-top:12px;">' +
            '<table style="width:100%;border-collapse:collapse;font-size:13px;">' +
              '<thead><tr style="position:sticky;top:0;background:var(--bg-hover);">' +
                '<th style="padding:8px 10px;width:34px;text-align:center;">' +
                  '<input type="checkbox" data-act="select-all" checked style="width:16px;height:16px;cursor:pointer;" />' +
                '</th>' +
                '<th data-sort="date" style="padding:8px 10px;width:90px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Data' + sortIcon('date') + '</th>' +
                '<th data-sort="categoryId" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Categoria' + sortIcon('categoryId') + '</th>' +
                '<th data-sort="costCenterId" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Centro de Custo' + sortIcon('costCenterId') + '</th>' +
                '<th data-sort="description" style="padding:8px 10px;text-align:left;font-size:11px;color:var(--text-muted);cursor:pointer;">Descrição' + sortIcon('description') + '</th>' +
                '<th data-sort="state" style="padding:8px 10px;text-align:center;font-size:11px;color:var(--text-muted);cursor:pointer;">Estado' + sortIcon('state') + '</th>' +
                '<th data-sort="amount" style="padding:8px 10px;text-align:right;font-size:11px;color:var(--text-muted);cursor:pointer;">Valor' + sortIcon('amount') + '</th>' +
              '</tr></thead>' +
              '<tbody>' + tableRows + '</tbody>' +
            '</table>' +
          '</div>'
        : '<p style="font-size:13px;color:var(--text-muted);margin-top:12px;">Nenhum lançamento encontrado no extrato.</p>';

      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + window.icon('check', 22) + '</span>' +
          '<div>' +
            '<p style="font-size:13px;color:var(--text-muted);">Extrato detectado</p>' +
            '<p style="font-size:18px;font-weight:800;">' + esc(issuerLabel) + '</p>' +
          '</div>' +
        '</div>' +
        accSelectorHtml +
        tableHtml +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:10px;">' +
          esc(rows.length + ' lançamento(s) encontrado(s). Escolha a conta, revise os estados e confirme a importação.') +
        '</p>'
      );

      m.$el.find('[data-region=account-select]').on('change', function () {
        selectedAccountId = this.value || null;
        refreshStatementPreview();
      });

      m.$el.find('[data-act=do-import]').hide();
      const $foot = m.$el.find('.modal-footer');
      $foot.find('[data-act=do-statement-confirm]').remove();
      if (rows.length) {
        $foot.append(window.btn({
          variant: 'primary', size: 'md', icon: 'check', label: 'Confirmar importação',
          attrs: 'data-act="do-statement-confirm" type="button"'
        }));
      }
    }

    // Re-runs the preview against the chosen account so duplicate/reconcile states refresh.
    function refreshStatementPreview() {
      if (!selectedFile) return;
      window.App.TransactionService.importPreview(selectedFile, lastPassword, selectedAccountId)
        .then(function (preview) {
          if (preview && preview.documentType === 'BANK_STATEMENT') {
            applyImportRules((preview && preview.rows) || []);
            showStatementPreview(preview);
          }
        })
        .catch(function (err) {
          window.toast((err && err.message) || 'Falha ao atualizar o preview', 'error');
        });
    }

    function confirmStatementImport() {
      if (!statementData) return;
      if (!selectedAccountId) { window.toast('Selecione a conta de destino', 'error'); return; }

      const rows = [];
      m.$el.find('[data-row-include]').each(function () {
        if (!this.checked) return;
        const idx = Number($(this).attr('data-idx'));
        const src = statementData.rows && statementData.rows[idx];
        if (!src) return;
        const $cat = m.$el.find('[data-row-category][data-idx="' + idx + '"]');
        const $costCenter = m.$el.find('[data-row-costcenter][data-idx="' + idx + '"]');
        const $desc = m.$el.find('[data-row-description][data-idx="' + idx + '"]');
        const categoryId = ($cat.val() || src.categoryId) || null;
        const costCenterId = ($costCenter.val() || src.costCenterId) || null;
        const description = ($desc.val() || src.description || '').trim();
        rows.push({
          description: description,
          amount: src.amount,
          date: src.date,
          transactionType: src.type,
          categoryId: categoryId,
          costCenterId: costCenterId,
        });
      });

      if (!rows.length) { window.toast('Selecione ao menos um lançamento', 'error'); return; }

      const $btn = m.$el.find('[data-act=do-statement-confirm]').prop('disabled', true);
      window.App.TransactionService.importConfirm({ type: 'BANK_STATEMENT', accountId: selectedAccountId, rows: rows })
        .then(function (res) {
          showConfirmSummary((res && res.created) || 0, (res && res.skipped) || 0, (res && res.reconciled) || 0);
          return onImported();
        })
        .catch(function (err) {
          $btn.prop('disabled', false);
          window.toast((err && err.message) || 'Falha ao confirmar a importação', 'error');
        });
    }

    // Parcelamento: as demais parcelas do grupo seguem a categoria/descrição da 1ª (campos travados);
    // aqui alinhamos os valores sugeridos pelo backend antes de qualquer edição do usuário.
    function alignGroupFields(rows) {
      const masterByGroup = {};
      rows.forEach(function (r) {
        if (r.groupId && r.installmentNumber === 1) masterByGroup[r.groupId] = { categoryId: r.categoryId, description: r.description };
      });
      rows.forEach(function (r) {
        const master = r.groupId && r.installmentNumber !== 1 ? masterByGroup[r.groupId] : null;
        if (!master) return;
        r.categoryId = master.categoryId;
        r.description = master.description;
      });
    }

    // Parcelamento: editar descrição/categoria da 1ª parcela do grupo replica para as demais.
    function propagateGroupEdit(idx, field, value) {
      if (!previewData || !previewData.rows) return;
      const row = previewData.rows[idx];
      if (!row || !row.groupId || row.installmentNumber !== 1) return;
      previewData.rows.forEach(function (r, i) {
        if (i === idx || r.groupId !== row.groupId) return;
        r[field] = value;
        if (field === 'description') {
          m.$el.find('[data-row-description][data-idx="' + i + '"]').val(value);
        } else if (field === 'categoryId') {
          m.$el.find('[data-row-category][data-idx="' + i + '"]').val(value);
        }
      });
    }

    m.$el.on('input', '[data-row-description]', function () {
      const idx = Number($(this).attr('data-idx'));
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].description = this.value;
      propagateGroupEdit(idx, 'description', this.value);
    });

    m.$el.on('change', '[data-row-category]', function () {
      const idx = Number($(this).attr('data-idx'));
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].categoryId = this.value;
      propagateGroupEdit(idx, 'categoryId', this.value);
    });

    m.$el.on('change', '[data-row-costcenter]', function () {
      const idx = Number($(this).attr('data-idx'));
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].costCenterId = this.value;
    });

    m.$el.on('click', '[data-act=toggle-password]', function () { revealPassword(''); });

    m.$el.on('click', '[data-act=do-confirm]', function () { confirmImport(); });

    m.$el.on('click', '[data-act=do-statement-confirm]', function () { confirmStatementImport(); });

    m.$el.on('click', '[data-act=do-import]', function () {
      if (!selectedFile) { window.toast('Selecione um arquivo PDF', 'error'); return; }
      const password = ($pwd.val() || '').trim();
      lastPassword = password || null;
      const $btn = $(this).prop('disabled', true);

      window.App.TransactionService.importPreview(selectedFile, lastPassword, null)
        .then(function (preview) { routePreview(preview); })
        .catch(function (err) {
          $btn.prop('disabled', false);
          const code = err && err.code;
          if (code === 'PASSWORD_REQUIRED') {
            revealPassword('Este PDF está protegido. Informe a senha para continuar.');
            return;
          }
          if (code === 'WRONG_PASSWORD') {
            revealPassword('Senha incorreta. Verifique e tente novamente.');
            return;
          }
          window.toast((err && err.message) || 'Falha ao importar a fatura', 'error');
        });
    });

    m.$el.on('change', '[data-act=select-all]', function () {
      const checked = this.checked;
      m.$el.find('[data-row-include]').prop('checked', checked);
    });

    m.$el.on('click', '[data-sort]', function () {
      const col = $(this).attr('data-sort');
      const isStatement = !!statementData;
      const data = isStatement ? statementData : previewData;
      const renderFn = isStatement ? showStatementPreview : showPreview;

      // Save current states before sorting
      m.$el.find('[data-row-include]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].checked = this.checked;
      });
      m.$el.find('[data-row-category]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].categoryId = this.value;
      });
      m.$el.find('[data-row-costcenter]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].costCenterId = this.value;
      });
      m.$el.find('[data-row-description]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].description = this.value;
      });
      m.$el.find('[data-row-card]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].cardId = this.value;
      });

      if (sortCol === col) {
        sortAsc = !sortAsc;
      } else {
        sortCol = col;
        sortAsc = true;
      }

      data.rows.sort(function (a, b) {
        let va = a[col];
        let vb = b[col];
        if (va == null) va = '';
        if (vb == null) vb = '';
        if (typeof va === 'string') va = va.toLowerCase();
        if (typeof vb === 'string') vb = vb.toLowerCase();

        if (va < vb) return sortAsc ? -1 : 1;
        if (va > vb) return sortAsc ? 1 : -1;
        return 0;
      });

      renderFn(data);
    });

    return m;
  };
})();

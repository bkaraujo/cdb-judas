/* feature/import-statement.js — fatia Importação de extrato/fatura em PDF. Um arquivo por fatia:
 * modal único que cobre dois fluxos, detectados automaticamente pelo backend a partir do PDF:
 *   - CREDIT_CARD_INVOICE: fatura de cartão (Santander/BTG) → escolhe cartão de destino.
 *   - BANK_STATEMENT: extrato de conta corrente (BTG) → escolhe conta + estado (novo/concilia/duplicado).
 *
 * Sem domain/application próprios: lê/escreve transações via TransactionsApi.{importPreview,
 * importConfirm} (fecha o lado transactions do V10/V11), regras de nomenclatura via
 * ImportRulesApi.{listCached,match} (fecha V11). PDFs protegidos pedem senha sob demanda
 * (códigos PASSWORD_REQUIRED / WRONG_PASSWORD).
 *
 * Uso (consumido por feature/transactions.js via ImportStatementApi.open, ver import-statement.api.js):
 *   window.ImportStatementApi.open({ onImported: function () { return loadTransactions(); } });
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

    // Fechamento contábil: linha cuja data cai no período fechado (backend manda `closed` por linha e
    // `closingPeriod` no preview) não pode virar transação — a confirmação inteira é recusada. A linha
    // fica fora da seleção (checkbox desmarcado e travado) e ganha o ícone de aviso com a explicação
    // no hover.
    function closedMessage(period) {
      return 'Período fechado' + (period ? ' até ' + period : '') +
        ': este lançamento não pode ser importado.';
    }

    function closedWarningHtml(period) {
      return '<span title="' + esc(closedMessage(period)) + '" ' +
        'style="color:var(--expense);display:inline-flex;vertical-align:middle;margin-left:6px;cursor:help;">' +
        window.icon('alertCircle', 14) + '</span>';
    }

    function statusTag(status) {
      const scheduled = status === 'scheduled';
      const label = scheduled ? 'Agendado' : 'Confirmado';
      const color = scheduled ? 'var(--text-muted)' : 'var(--income)';
      return '<span style="font-size:11px;color:' + color + ';">' + esc(label) + '</span>';
    }

    // Import category set for a given movement type (statement rows can be income or expense).
    // Memoizado por tipo dentro do passe de render: flatCategories monta o rótulo encadeado de
    // cada categoria e ordena com localeCompare — rodar isso por linha, numa fatura com centenas
    // de linhas, é o custo dominante do preview. keepId (categoria inativa da linha) só é raro,
    // então o cache serve quem não precisa dele e os demais recalculam.
    let catCache = {};

    function importCategoriesFor(type, keepId) {
      const key = type === 'income' ? 'income' : 'expense';
      const cached = catCache[key];
      const covered = cached && (!keepId || cached.some(function (c) { return String(c.id) === String(keepId); }));
      if (covered) return cached;
      const nature = key === 'income' ? 'INCOME' : 'EXPENSE';
      const byNature = flatCategories(nature, true, keepId);
      const list = byNature.length ? byNature : flatCategories(null, true, keepId);
      if (!keepId) catCache[key] = list;
      return list;
    }

    // Quick-create no cabeçalho do preview (categoria/tag) não é por linha — pra a criação ficar
    // elegível em todas as linhas na hora, sem esperar o round-trip do SSE, entra direto no cache
    // com a mesma forma que sse-client.applyUpsert usaria (dedupe por id: quando o evento real
    // chegar, só substitui a mesma entrada, sem duplicar).
    function upsertLocalCache(key, item) {
      window.CBD = window.CBD || {};
      const list = Array.isArray(window.CBD[key]) ? window.CBD[key].slice() : [];
      const idx = list.findIndex(function (x) { return String(x.id) === String(item.id); });
      if (idx >= 0) list[idx] = item; else list.push(item);
      window.CBD[key] = list;
    }

    function typeSelectHtml(selectedType, idx) {
      const type = selectedType === 'income' ? 'income' : 'expense';
      return '<select data-row-type data-idx="' + idx + '" ' +
        'style="width:auto;font-size:12px;padding:4px 6px;">' +
          '<option value="expense"' + (type === 'expense' ? ' selected' : '') + '>Despesa</option>' +
          '<option value="income"' + (type === 'income' ? ' selected' : '') + '>Receita</option>' +
        '</select>';
    }

    // Lista de opções da linha com placeholder explícito ("Selecione") quando não há seleção
    // válida — sem isso, o browser marca a 1ª opção como selecionada por conta própria (visualmente
    // e em .val()), indistinguível de uma escolha real do usuário. Uma lista só, consumida pelo
    // <select> nativo e pelo combobox por cima dele, pra os dois nunca divergirem.
    function categoryItems(cats, selectedId) {
      if (!cats.length) return [{ value: '', label: 'Sem categorias' }];
      const items = cats.map(function (c) { return { value: c.id, label: c.label }; });
      const hasSel = cats.some(function (c) { return String(c.id) === String(selectedId); });
      return hasSel ? items : [{ value: '', label: 'Selecione' }].concat(items);
    }

    function categoryOptionsHtml(cats, selectedId) {
      const target = String(selectedId == null ? '' : selectedId);
      return categoryItems(cats, selectedId).map(function (it) {
        const sel = String(it.value) === target ? ' selected' : '';
        return '<option value="' + esc(it.value) + '"' + sel + '>' + esc(it.label) + '</option>';
      }).join('');
    }

    // Rebuilds the category <select> of a row so its options track the row's current type
    // (income/expense); called on load and whenever the user flips the type dropdown.
    function refreshCategoryOptions(idx, type) {
      const $cat = m.$el.find('[data-row-category][data-idx="' + idx + '"]');
      if (!$cat.length) return;
      const currentId = $cat.val();
      const cats = importCategoriesFor(type, currentId);
      const stillValid = cats.some(function (c) { return String(c.id) === String(currentId); });
      const selId = stillValid ? currentId : '';
      $cat.html(categoryOptionsHtml(cats, selId)).prop('disabled', !cats.length);
      window.refreshSearchSelect(catSelectId(idx));
      const data = previewData || statementData;
      if (data && data.rows && data.rows[idx]) data.rows[idx].categoryId = selId || null;
    }

    // O <select> nativo continua sendo a fonte da verdade da linha (é o que confirmImport, o
    // save-antes-do-sort e propagateGroupEdit leem/escrevem); some da tela e ganha o combobox com
    // busca por cima — a mesma pele da tela de edição de lançamento. `lazy`: as linhas do painel
    // só são montadas ao abrir, senão uma fatura de centenas de linhas dobraria o DOM de opções.
    // `floating`: a tabela vive dentro de um container overflow:scroll, que recortaria um painel
    // position:absolute.
    function catSelectId(idx) {
      return 'imp-cat-' + uniq + '-' + idx;
    }

    function categoryComboHtml(cats, selectedId, idx, extraSelectAttrs, locked) {
      const id = catSelectId(idx);
      const title = locked ? 'Segue a categoria da 1ª parcela do grupo' : '';
      return window.categoryPickerHtml({
        items: cats.map(function (c) { return { value: c.id, label: c.label }; }),
        selectedId: selectedId,
        selectId: id,
        selectAttrs: ' data-row-category data-idx="' + idx + '"' + extraSelectAttrs +
          (cats.length ? '' : ' disabled'),
        lazy: true,
        floating: true,
        compact: true,
        disabled: locked || !cats.length,
        title: title,
      });
    }

    function categorySelectHtml(selectedId, idx, groupId, locked, type) {
      const groupAttr = ' data-group-id="' + esc(groupId || '') + '"';
      const lockedAttrs = locked ? ' disabled title="Segue a categoria da 1ª parcela do grupo"' : '';
      return categoryComboHtml(importCategoriesFor(type, selectedId), selectedId, idx,
        groupAttr + lockedAttrs, locked);
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
      const rules = window.ImportRulesApi.listCached();
      if (!rules.length) return;
      rows.forEach(function (row) {
        const rule = window.ImportRulesApi.match(row.description, rules);
        if (!rule) return;
        row.description = rule.name;
        if (rule.categoryId) row.categoryId = rule.categoryId;
        if (rule.costCenterId) row.costCenterId = rule.costCenterId;
      });
    }

    // Botões "+ Nova categoria" / "+ Nova tag" no cabeçalho do preview (não por linha): a fatura
    // inteira compartilha os dois catálogos, e um par só evita repetir o botão em cada linha.
    function quickCreateButtonsHtml() {
      return '<div style="display:flex;gap:16px;flex-shrink:0;">' +
        '<button type="button" data-act="new-category" ' +
          'style="background:none;border:none;color:var(--accent);cursor:pointer;' +
          'font-size:12px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' +
          window.icon('plus', 12) + 'Nova categoria' +
        '</button>' +
        '<button type="button" data-act="new-tag" ' +
          'style="background:none;border:none;color:var(--accent);cursor:pointer;' +
          'font-size:12px;font-weight:600;padding:0;display:inline-flex;align-items:center;gap:3px;">' +
          window.icon('plus', 12) + 'Nova tag' +
        '</button>' +
      '</div>';
    }

    // Lê de volta pro `data.rows` os campos que só são relidos do DOM no confirm/na ordenação —
    // precisa rodar antes de qualquer re-render (sort ou quick-create), senão a edição em curso
    // na tela se perde no rebuild da tabela.
    function syncRowsFromDom(data) {
      m.$el.find('[data-row-include]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].checked = this.checked;
      });
      m.$el.find('[data-row-type]').each(function () {
        const idx = Number($(this).attr('data-idx'));
        data.rows[idx].type = this.value;
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

    // ── Tabela de preview table-driven ──────────────────────────
    // CREDIT_CARD_INVOICE (fatura) e BANK_STATEMENT (extrato) renderizam a mesma grade de campos,
    // com pequenas diferenças (parcela/cartão/status só na fatura, estado só no extrato, cor do
    // valor só no extrato). Cada coluna é definida uma vez; consertar um campo aqui vale pros dois
    // fluxos, sem precisar lembrar de replicar a mudança numa 2ª função quase-idêntica.
    function isDuplicateRow(kind, row) {
      return kind === 'CREDIT_CARD_INVOICE' ? !!row.duplicate : row.state === 'DUPLICATE';
    }

    function isGroupLocked(row) {
      return !!(row.groupId && row.installmentNumber != null && row.installmentNumber !== 1);
    }

    function rowCheckedAttr(kind, row) {
      if (row.closed) return '';
      if (row.checked !== undefined) return row.checked ? 'checked' : '';
      return isDuplicateRow(kind, row) ? '' : 'checked';
    }

    function descriptionSuffix(kind, row) {
      if (kind === 'CREDIT_CARD_INVOICE') {
        return row.duplicate
          ? ' <span style="color:var(--text-muted);font-size:11px;font-style:italic;">já importado</span>'
          : '';
      }
      return (row.state === 'RECONCILE' && row.reconcileDescription)
        ? ' <span style="color:var(--text-muted);font-size:11px;font-style:italic;">↔ ' + esc(row.reconcileDescription) + '</span>'
        : '';
    }

    function amountCellHtml(kind, row) {
      if (kind === 'BANK_STATEMENT') {
        const amt = Number(row.amount) || 0;
        const color = amt < 0 ? 'var(--expense)' : 'var(--income)';
        return '<span style="color:' + color + ';">' + esc(fmt(amt)) + '</span>';
      }
      return esc(fmt(row.amount));
    }

    function colInclude() {
      return {
        label: '', sortable: false, align: 'center', width: '34px',
        thHtml: '<input type="checkbox" data-act="select-all" checked style="width:16px;height:16px;cursor:pointer;" />',
        cell: function (row, idx, ctx) {
          const closed = !!row.closed;
          const closingPeriod = ctx.data && ctx.data.closingPeriod;
          return '<input type="checkbox" data-row-include data-idx="' + idx + '" ' + rowCheckedAttr(ctx.kind, row) +
            (closed ? ' disabled title="' + esc(closedMessage(closingPeriod)) + '"' : '') + ' ' +
            'style="width:16px;height:16px;cursor:pointer;" />';
        },
      };
    }
    function colDate() {
      return {
        key: 'date', label: 'Data', sortable: true, align: 'left', width: '90px', tdWidth: true,
        tdExtra: 'white-space:nowrap;color:var(--text-secondary);',
        cell: function (row, idx, ctx) {
          const closingPeriod = ctx.data && ctx.data.closingPeriod;
          return esc(fmtIsoDate(row.date)) + (row.closed ? closedWarningHtml(closingPeriod) : '');
        },
      };
    }
    function colType() {
      return {
        key: 'type', label: 'Tipo', sortable: true, align: 'left', width: '',
        cell: function (row, idx) { return typeSelectHtml(row.type, idx); },
      };
    }
    function colCategory() {
      return {
        key: 'categoryId', label: 'Categoria', sortable: true, align: 'left', width: '',
        cell: function (row, idx) {
          return categorySelectHtml(row.categoryId, idx, row.groupId, isGroupLocked(row), row.type);
        },
      };
    }
    function colTags() {
      return {
        key: null, label: 'Tags', sortable: false, align: 'left', width: '',
        cell: function (row, idx) {
          const locked = isGroupLocked(row);
          return window.tagsDropdownHtml(row.tagIds, idx, {
            floating: true, compact: true, disabled: locked,
            title: locked ? 'Segue as tags da 1ª parcela do grupo' : ''
          });
        },
      };
    }
    function colCostCenter() {
      return {
        key: 'costCenterId', label: 'Centro de Custo', sortable: true, align: 'left', width: '',
        cell: function (row, idx) { return costCenterSelectHtml(row.costCenterId, idx); },
      };
    }
    function colInstallment() {
      return {
        key: 'installmentNumber', label: 'Parcela', sortable: true, align: 'center', width: '',
        tdExtra: 'color:var(--text-secondary);',
        cell: function (row) {
          const has = row.installmentNumber != null && row.installmentTotal != null;
          return has ? esc(row.installmentNumber + '/' + row.installmentTotal) : '—';
        },
      };
    }
    function colDescription() {
      return {
        key: 'description', label: 'Descrição', sortable: true, align: 'left', width: '',
        cell: function (row, idx, ctx) {
          const locked = isGroupLocked(row);
          const descLockedAttrs = locked ? ' readonly title="Segue a descrição da 1ª parcela do grupo"' : '';
          return '<input type="text" data-row-description data-idx="' + idx + '" data-group-id="' + esc(row.groupId || '') + '" value="' + esc(row.description) + '"' + descLockedAttrs + ' ' +
              'style="width:100%;font-size:12px;padding:4px 6px;border:1px solid transparent;background:transparent;color:inherit;outline:none;text-transform:uppercase;' + (locked ? 'opacity:0.6;' : '') + '" ' +
              'onfocus="this.style.border=\'1px solid var(--border)\';this.style.background=\'var(--bg-card)\'" ' +
              'onblur="this.style.border=\'1px solid transparent\';this.style.background=\'transparent\'" />' +
            descriptionSuffix(ctx.kind, row);
        },
      };
    }
    function colCard() {
      return {
        key: 'suggestedCardId', label: 'Cartão', sortable: true, align: 'left', width: '',
        cell: function (row, idx) {
          const cardSelected = row.cardId !== undefined ? row.cardId : row.suggestedCardId;
          return cardSelectHtml(cardSelected, idx);
        },
      };
    }
    function colStatus() {
      return {
        key: 'status', label: 'Status', sortable: true, align: 'center', width: '',
        tdExtra: 'white-space:nowrap;',
        cell: function (row) { return statusTag(row.status); },
      };
    }
    function colState() {
      return {
        key: 'state', label: 'Estado', sortable: true, align: 'center', width: '',
        tdExtra: 'white-space:nowrap;',
        cell: function (row) { return stateBadge(row.state); },
      };
    }
    function colAmount() {
      return {
        key: 'amount', label: 'Valor', sortable: true, align: 'right', width: '',
        tdExtra: 'font-weight:700;white-space:nowrap;',
        cell: function (row, idx, ctx) { return amountCellHtml(ctx.kind, row); },
      };
    }

    const COLUMNS = {
      CREDIT_CARD_INVOICE: [
        colInclude(), colDate(), colType(), colCategory(), colTags(), colCostCenter(),
        colInstallment(), colDescription(), colCard(), colStatus(), colAmount(),
      ],
      BANK_STATEMENT: [
        colInclude(), colDate(), colType(), colCategory(), colTags(), colCostCenter(),
        colDescription(), colState(), colAmount(),
      ],
    };

    function sortIcon(col) {
      if (sortCol !== col) return '';
      return sortAsc ? ' ↑' : ' ↓';
    }

    function theadCellHtml(col) {
      if (col.thHtml) {
        return '<th style="' + (col.width ? 'width:' + col.width + ';' : '') + 'text-align:' + col.align + ';">' + col.thHtml + '</th>';
      }
      const sortAttr = col.sortable ? ' data-sort="' + col.key + '"' : '';
      const cursor = col.sortable ? 'cursor:pointer;' : '';
      const widthStyle = col.width ? 'width:' + col.width + ';' : '';
      const icon = col.sortable ? sortIcon(col.key) : '';
      return '<th' + sortAttr + ' style="' + widthStyle + 'text-align:' + col.align + ';' + cursor + '">' + esc(col.label) + icon + '</th>';
    }

    function tdCellHtml(col, row, idx, ctx) {
      const widthStyle = (col.tdWidth && col.width) ? 'width:' + col.width + ';' : '';
      return '<td style="' + widthStyle + 'text-align:' + col.align + ';' + (col.tdExtra || '') + '">' + col.cell(row, idx, ctx) + '</td>';
    }

    function renderPreviewTable(kind, data) {
      const rows = (data && data.rows) || [];
      if (!rows.length) {
        return '<p style="font-size:13px;color:var(--text-muted);margin-top:12px;">' +
          (kind === 'CREDIT_CARD_INVOICE' ? 'Nenhum lançamento encontrado na fatura.' : 'Nenhum lançamento encontrado no extrato.') +
          '</p>';
      }
      const cols = COLUMNS[kind];
      const ctx = { kind: kind, data: data };
      const theadRow = cols.map(theadCellHtml).join('');
      const tbodyRows = rows.map(function (row, idx) {
        const cls = (isDuplicateRow(kind, row) || row.closed) ? ' style="opacity:0.6;"' : '';
        return '<tr' + cls + '>' + cols.map(function (col) { return tdCellHtml(col, row, idx, ctx); }).join('') + '</tr>';
      }).join('');
      return '<div style="flex:1;overflow-y:scroll;min-height:0;border:1px solid var(--border);border-radius:var(--radius-sm);margin-top:12px;">' +
          '<table class="import-table">' +
            '<thead><tr class="import-table-head-row">' + theadRow + '</tr></thead>' +
            '<tbody>' + tbodyRows + '</tbody>' +
          '</table>' +
        '</div>';
    }

    // ── Preview orchestration ────────────────────────────────────
    function renderPreview(kind, data) {
      if (kind === 'CREDIT_CARD_INVOICE') previewData = data; else statementData = data;
      catCache = {};
      const rows = (data && data.rows) || [];
      const issuer = (data && data.issuer) || 'UNKNOWN';
      const issuerLabel = issuer === 'SANTANDER' ? 'Santander' : issuer === 'BTG' ? 'BTG Pactual' : issuer;

      let bannerTitle, bannerExtra, belowBanner, confirmAct, footerMsg;

      if (kind === 'CREDIT_CARD_INVOICE') {
        alignGroupFields(rows);
        const candidateCards = (data && data.candidateCards) || [];
        cardCandidates = candidateCards;
        const last4s = (data && data.last4s) || [];
        bannerTitle = 'Banco detectado';
        bannerExtra = last4s.length
          ? '<p style="font-size:12px;color:var(--text-muted);margin-top:2px;">' +
              'Cartões: ' + esc(last4s.map(function (l) { return '•••• ' + l; }).join('  ')) +
            '</p>'
          : '';
        // Sem cartão padrão: o cartão é escolhido por linha. Se nenhum cartão da fatura está
        // cadastrado, não há como atribuir os lançamentos — avisa o usuário.
        belowBanner = (!candidateCards.length && rows.length)
          ? '<p style="font-size:12px;color:var(--expense);margin-top:12px;">' +
              'Nenhum cartão cadastrado corresponde aos 4 últimos dígitos desta fatura. ' +
              'Cadastre o cartão para importar os lançamentos.' +
            '</p>'
          : '';
        confirmAct = 'do-confirm';
        footerMsg = rows.length + ' lançamento(s) encontrado(s). Revise, ajuste as categorias e o cartão de cada linha e confirme a importação.';
      } else {
        const accounts = (data && data.candidateAccounts) || [];
        // Backend só resolve selectedAccountId quando inequívoco (accountId explícito ou única
        // conta ativa). Sem isso, não há dado algum ligando o issuer detectado a uma conta — não
        // adivinhar, forçar escolha.
        selectedAccountId = (data && data.selectedAccountId) || selectedAccountId || null;
        bannerTitle = 'Extrato detectado';
        bannerExtra = '';
        const accOptions = (selectedAccountId ? '' : '<option value="" selected>Selecione a conta</option>') +
          accounts.map(function (a) {
            const sel = String(a.id) === String(selectedAccountId) ? ' selected' : '';
            return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
          }).join('');
        belowBanner = accounts.length
          ? '<div class="form-group" style="margin-top:12px;">' +
              '<label class="form-label" for="' + cardSelectId + '">Conta de destino</label>' +
              '<select id="' + cardSelectId + '" data-region="account-select">' + accOptions + '</select>' +
            '</div>'
          : '<p style="font-size:12px;color:var(--expense);margin-top:12px;">Nenhuma conta disponível para importação.</p>';
        confirmAct = 'do-statement-confirm';
        footerMsg = rows.length + ' lançamento(s) encontrado(s). Escolha a conta, revise os estados e confirme a importação.';
      }

      m.$body.html(
        '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;">' +
          '<span style="color:var(--income);display:flex;">' + window.icon('check', 22) + '</span>' +
          '<div style="flex:1;">' +
            '<p style="font-size:13px;color:var(--text-muted);">' + esc(bannerTitle) + '</p>' +
            '<p style="font-size:18px;font-weight:800;">' + esc(issuerLabel) + '</p>' +
            bannerExtra +
          '</div>' +
          quickCreateButtonsHtml() +
        '</div>' +
        belowBanner +
        renderPreviewTable(kind, data) +
        '<p style="font-size:12px;color:var(--text-muted);margin-top:10px;">' + esc(footerMsg) + '</p>'
      );

      if (kind === 'BANK_STATEMENT') {
        m.$el.find('[data-region=account-select]').on('change', function () {
          selectedAccountId = this.value || null;
          refreshStatementPreview();
        });
      }

      // Swap the upload action for a confirm action (only when there are rows to import).
      m.$el.find('[data-act=do-import]').hide();
      const $foot = m.$el.find('.modal-footer');
      $foot.find('[data-act=do-confirm],[data-act=do-statement-confirm]').remove();
      if (rows.length) {
        $foot.append(window.btn({
          variant: 'primary', size: 'md', icon: 'check', label: 'Confirmar importação',
          attrs: 'data-act="' + confirmAct + '" type="button"'
        }));
      }
    }

    // Coleta as linhas marcadas do DOM (fonte de verdade em edição), comum aos dois fluxos —
    // só a fatura exige cardId por linha e carrega os campos de parcela.
    function collectRows(kind) {
      const src = kind === 'CREDIT_CARD_INVOICE' ? previewData : statementData;
      const rows = [];
      let missingCard = false;
      let missingCategory = false;
      m.$el.find('[data-row-include]').each(function () {
        if (!this.checked) return;
        const idx = Number($(this).attr('data-idx'));
        const s = src && src.rows && src.rows[idx];
        if (!s) return;
        const $cat = m.$el.find('[data-row-category][data-idx="' + idx + '"]');
        const $costCenter = m.$el.find('[data-row-costcenter][data-idx="' + idx + '"]');
        const $desc = m.$el.find('[data-row-description][data-idx="' + idx + '"]');
        const categoryId = $cat.val() || null;
        if (!categoryId) { missingCategory = true; }
        const costCenterId = ($costCenter.val() || s.costCenterId) || null;
        const description = ($desc.val() || s.description || '').trim();
        const row = {
          description: description,
          amount: s.amount,
          date: s.date,
          transactionType: s.type,
          categoryId: categoryId,
          costCenterId: costCenterId,
          tagIds: s.tagIds || [],
        };
        if (kind === 'CREDIT_CARD_INVOICE') {
          // O cartão é definido por transação — cada linha incluída precisa de um cartão.
          const $card = m.$el.find('[data-row-card][data-idx="' + idx + '"]');
          const cardId = $card.length ? ($card.val() || null) : null;
          if (!cardId) { missingCard = true; }
          row.originalDate = s.originalDate;
          row.installmentNumber = s.installmentNumber;
          row.installmentTotal = s.installmentTotal;
          row.cardId = cardId;
        }
        rows.push(row);
      });
      return { rows: rows, missingCard: missingCard, missingCategory: missingCategory };
    }

    function confirmImport() {
      if (!previewData) return;
      const collected = collectRows('CREDIT_CARD_INVOICE');
      if (!collected.rows.length) { window.toast('Selecione ao menos um lançamento', 'error'); return; }
      if (collected.missingCard) { window.toast('Selecione o cartão de cada lançamento', 'error'); return; }
      if (collected.missingCategory) { window.toast('Selecione a categoria de cada lançamento', 'error'); return; }

      const $btn = m.$el.find('[data-act=do-confirm]');
      window.runMutation(window.TransactionsApi.importConfirm({ type: 'CREDIT_CARD_INVOICE', rows: collected.rows }), {
        $btn: $btn,
        failure: 'Falha ao confirmar a importação',
        onDone: function (res) {
          const created = (res && res.created) || 0;
          const skipped = (res && res.skipped) || 0;
          showConfirmSummary(created, skipped);
          return onImported();
        },
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
      const kind = (preview && preview.documentType === 'BANK_STATEMENT') ? 'BANK_STATEMENT' : 'CREDIT_CARD_INVOICE';
      renderPreview(kind, preview);
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

    // Re-runs the preview against the chosen account so duplicate/reconcile states refresh.
    function refreshStatementPreview() {
      if (!selectedFile) return;
      window.runMutation(window.TransactionsApi.importPreview(selectedFile, lastPassword, selectedAccountId), {
        failure: 'Falha ao atualizar o preview',
        onDone: function (preview) {
          if (preview && preview.documentType === 'BANK_STATEMENT') {
            applyImportRules((preview && preview.rows) || []);
            renderPreview('BANK_STATEMENT', preview);
          }
        },
      });
    }

    function confirmStatementImport() {
      if (!statementData) return;
      if (!selectedAccountId) { window.toast('Selecione a conta de destino', 'error'); return; }
      const collected = collectRows('BANK_STATEMENT');
      if (!collected.rows.length) { window.toast('Selecione ao menos um lançamento', 'error'); return; }
      if (collected.missingCategory) { window.toast('Selecione a categoria de cada lançamento', 'error'); return; }

      const $btn = m.$el.find('[data-act=do-statement-confirm]');
      window.runMutation(window.TransactionsApi.importConfirm({ type: 'BANK_STATEMENT', accountId: selectedAccountId, rows: collected.rows }), {
        $btn: $btn,
        failure: 'Falha ao confirmar a importação',
        onDone: function (res) {
          showConfirmSummary((res && res.created) || 0, (res && res.skipped) || 0, (res && res.reconciled) || 0);
          return onImported();
        },
      });
    }

    // Parcelamento: as demais parcelas do grupo seguem a categoria/descrição da 1ª (campos travados);
    // aqui alinhamos os valores sugeridos pelo backend antes de qualquer edição do usuário.
    function alignGroupFields(rows) {
      const masterByGroup = {};
      rows.forEach(function (r) {
        if (r.groupId && r.installmentNumber === 1) {
          masterByGroup[r.groupId] = { categoryId: r.categoryId, description: r.description, type: r.type, tagIds: (r.tagIds || []).slice() };
        }
      });
      rows.forEach(function (r) {
        const master = r.groupId && r.installmentNumber !== 1 ? masterByGroup[r.groupId] : null;
        if (!master) return;
        r.categoryId = master.categoryId;
        r.description = master.description;
        r.type = master.type;
        r.tagIds = master.tagIds.slice();
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
          window.refreshSearchSelect(catSelectId(i)); // o combobox por cima mostra o rótulo, não o valor
        } else if (field === 'type') {
          m.$el.find('[data-row-type][data-idx="' + i + '"]').val(value);
          refreshCategoryOptions(i, value);
        } else if (field === 'tagIds') {
          const ids = (value || []).map(String);
          const $checks = m.$el.find('[data-region=tags-dropdown][data-idx="' + i + '"] [data-tag-check]');
          $checks.each(function () {
            $(this).prop('checked', ids.indexOf(String($(this).attr('data-tag-id'))) !== -1);
          });
          if ($checks.length) window.refreshTagsDropdownLabel($checks.first());
        }
      });
    }

    m.$el.on('input', '[data-row-description]', function () {
      const idx = Number($(this).attr('data-idx'));
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].description = this.value;
      propagateGroupEdit(idx, 'description', this.value);
    });

    m.$el.on('change', '[data-row-type]', function () {
      const idx = Number($(this).attr('data-idx'));
      const value = this.value;
      const data = previewData || statementData;
      if (data && data.rows && data.rows[idx]) data.rows[idx].type = value;
      refreshCategoryOptions(idx, value);
      propagateGroupEdit(idx, 'type', value);
    });

    m.$el.on('change', '[data-row-category]', function () {
      const idx = Number($(this).attr('data-idx'));
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].categoryId = this.value;
      propagateGroupEdit(idx, 'categoryId', this.value);
    });

    m.$el.on('change', '[data-tag-check]', function () {
      const idx = Number($(this).attr('data-idx'));
      const tagId = String($(this).attr('data-tag-id'));
      const data = previewData || statementData;
      if (!data || !data.rows || !data.rows[idx]) return;
      const row = data.rows[idx];
      const current = (row.tagIds || []).map(String);
      row.tagIds = this.checked ? current.concat([tagId]) : current.filter(function (id) { return id !== tagId; });
      window.refreshTagsDropdownLabel($(this));
      propagateGroupEdit(idx, 'tagIds', row.tagIds);
    });

    m.$el.on('change', '[data-row-costcenter]', function () {
      const idx = Number($(this).attr('data-idx'));
      if (previewData && previewData.rows && previewData.rows[idx]) previewData.rows[idx].costCenterId = this.value;
    });

    // Quick-create no topo: sem linha de origem, o Tipo (Despesa/Receita) fica editável no modal
    // (`nature: null`). O re-render inteiro reaproveita o mesmo caminho da ordenação (sort handler).
    function reRenderPreview() {
      const data = previewData || statementData;
      if (!data) return;
      syncRowsFromDom(data);
      renderPreview(statementData ? 'BANK_STATEMENT' : 'CREDIT_CARD_INVOICE', data);
    }

    m.$el.on('click', '[data-act=new-category]', function (e) {
      e.preventDefault();
      window.openCategoryCreateModal(null, function (created) {
        upsertLocalCache('categories', created);
        catCache = {};
        reRenderPreview();
      });
    });

    m.$el.on('click', '[data-act=new-tag]', function (e) {
      e.preventDefault();
      window.openTagCreateModal(function (created) {
        upsertLocalCache('tags', created);
        reRenderPreview();
      });
    });

    m.$el.on('click', '[data-act=toggle-password]', function () { revealPassword(''); });

    m.$el.on('click', '[data-act=do-confirm]', function () { confirmImport(); });

    m.$el.on('click', '[data-act=do-statement-confirm]', function () { confirmStatementImport(); });

    m.$el.on('click', '[data-act=do-import]', function () {
      if (!selectedFile) { window.toast('Selecione um arquivo PDF', 'error'); return; }
      const password = ($pwd.val() || '').trim();
      lastPassword = password || null;
      const $btn = $(this);

      window.runMutation(window.TransactionsApi.importPreview(selectedFile, lastPassword, null), {
        $btn: $btn,
        failure: 'Falha ao importar a fatura',
        onDone: function (preview) { routePreview(preview); },
        onError: function (err) {
          const code = err && err.code;
          if (code === 'PASSWORD_REQUIRED') {
            revealPassword('Este PDF está protegido. Informe a senha para continuar.');
            return true;
          }
          if (code === 'WRONG_PASSWORD') {
            revealPassword('Senha incorreta. Verifique e tente novamente.');
            return true;
          }
        },
      });
    });

    m.$el.on('change', '[data-act=select-all]', function () {
      const checked = this.checked;
      // Linha em período fechado tem o checkbox travado: marcar tudo nunca a inclui.
      m.$el.find('[data-row-include]').not(':disabled').prop('checked', checked);
    });

    m.$el.on('click', '[data-sort]', function () {
      const col = $(this).attr('data-sort');
      const isStatement = !!statementData;
      const data = isStatement ? statementData : previewData;
      const kind = isStatement ? 'BANK_STATEMENT' : 'CREDIT_CARD_INVOICE';

      // Save current states before sorting
      syncRowsFromDom(data);

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

      renderPreview(kind, data);
    });

    return m;
  };
})();

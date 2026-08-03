/* pages/import-rules.js — Regras de nomenclatura (nome → conta/categoria/centro de custo).
 *
 * Sem SSE (a fatia f010 não dispara evento nenhum — ver web/CLAUDE.md): toda mutação recarrega
 * a lista via App.ImportRuleService.list() em vez de depender de App.CacheStore/onChange, mesmo
 * padrão de budget.js.
 */
(function () {
  window.Pages = window.Pages || {};

  let state = null;

  function resetState() {
    state = {
      rules: [],
      $root: null,
    };
  }

  function findRule(id) { return window.byId(state.rules, id); }

  // No SSE for this slice (see header comment) — this page is the only place regras de
  // nomenclatura get mutated, so it's also the only place that can keep window.CBD.importRules
  // fresh for the live matcher used elsewhere (manual lançamento form, import previews).
  function loadRules() {
    return window.App.ImportRuleService.list().then(function (list) {
      state.rules = Array.isArray(list) ? list : [];
      window.CBD = window.CBD || {};
      window.CBD.importRules = state.rules;
      render();
    });
  }

  function accountName(id) {
    if (!id) return null;
    const a = window.App.CacheStore.findById('accounts', id);
    return a ? a.name : null;
  }

  function categoryName(id) {
    if (!id) return null;
    const c = window.App.CacheStore.findById('categories', id);
    return c ? window.categoryLabel(c) : null;
  }

  function costCenterName(id) {
    if (!id) return null;
    const cc = window.App.CacheStore.findById('costCenters', id);
    return cc ? (cc.description || cc.name || '') : null;
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $header = $(
      '<div class="page-header">' +
        '<h1>Regras de Nomenclatura</h1>' +
        '<div class="page-header-actions" data-region="actions"></div>' +
      '</div>'
    );
    $header.find('[data-region=actions]').append(
      window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Regra',
        attrs: 'data-act="new"'
      })
    );

    let $body;
    if (!state.rules.length) {
      $body = $(window.emptyState({
        icon: 'edit',
        title: 'Nenhuma regra cadastrada',
        desc: 'Ao aparecer o texto de uma regra na descrição de um lançamento (digitado ou ' +
          'importado de PDF), a descrição é substituída e conta/categoria/centro de custo podem ' +
          'ser preenchidos automaticamente. Clique em "Nova Regra" para começar.'
      }));
    } else {
      const sorted = state.rules.slice().sort(window.sortByName);
      $body = $('<div class="card card-list" style="overflow-y:auto;max-height:calc(100vh - 200px);min-height:200px;"></div>');
      sorted.forEach(function (r) { $body.append(renderRow(r)); });
    }

    $root.empty().append($header).append($body);
  }

  function renderRow(r) {
    const parts = [];
    const acc = accountName(r.accountId);
    const cat = categoryName(r.categoryId);
    const cc = costCenterName(r.costCenterId);
    if (acc) parts.push('Conta: ' + acc);
    if (cat) parts.push('Categoria: ' + cat);
    if (cc) parts.push('Centro de custo: ' + cc);
    const subtitle = parts.length ? parts.join(' · ') : 'Sem conta/categoria/centro de custo';

    const $row = $(
      '<div class="card-row" data-id="' + esc(r.id) + '">' +
        '<div class="card-row-main">' +
          '<div style="min-width:0;display:flex;flex-direction:column;gap:2px;">' +
            '<span class="row-title" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
              esc(r.name) +
            '</span>' +
            '<span class="row-sub" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
              esc(subtitle) +
            '</span>' +
          '</div>' +
        '</div>' +
        '<div class="card-row-actions" data-region="row-actions"></div>' +
      '</div>'
    );

    $row.find('[data-region=row-actions]')
      .append(window.rowActionBtn('edit',  'Editar',  r.id))
      .append(window.rowActionBtn('trash', 'Excluir', r.id, true));

    return $row;
  }

  // ── Modal: create / edit ──────────────────────────────────
  function optionsHtml(items, selectedId, labelFn) {
    const options = ['<option value="">— Nenhuma —</option>'];
    items.forEach(function (it) {
      const sel = String(it.id) === String(selectedId) ? ' selected' : '';
      options.push('<option value="' + esc(it.id) + '"' + sel + '>' + esc(labelFn(it)) + '</option>');
    });
    return options.join('');
  }

  function openFormModal(existing) {
    const isEdit = !!existing;
    const uniq = Date.now();
    const ids = {
      name: 'rule-name-' + uniq,
      account: 'rule-account-' + uniq,
      category: 'rule-category-' + uniq,
      costCenter: 'rule-cc-' + uniq,
    };
    const initial = {
      name: isEdit ? (existing.name || '') : '',
      accountId: isEdit ? (existing.accountId || '') : '',
      categoryId: isEdit ? (existing.categoryId || '') : '',
      costCenterId: isEdit ? (existing.costCenterId || '') : '',
    };

    const accountOptions = optionsHtml(window.accountsList(), initial.accountId, function (a) { return a.name; });
    const categoryOptions = optionsHtml(window.flatCategories(null, true, initial.categoryId), initial.categoryId, function (c) { return c.label; });
    const costCenterOptions = optionsHtml(window.App.CacheStore.costCenters(), initial.costCenterId, function (c) { return c.description || c.name || ''; });

    const bodyHtml =
      '<form data-form="rule" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.name + '">Nome</label>' +
            '<input id="' + ids.name + '" name="name" type="text" required minlength="3" ' +
              'placeholder="Ex: Companhia de Saneamento" value="' + esc(initial.name) + '" />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.account + '">Conta (opcional)</label>' +
            '<select id="' + ids.account + '" name="accountId">' + accountOptions + '</select>' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.category + '">Categoria (opcional)</label>' +
            '<select id="' + ids.category + '" name="categoryId">' + categoryOptions + '</select>' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.costCenter + '">Centro de custo (opcional)</label>' +
            '<select id="' + ids.costCenter + '" name="costCenterId">' + costCenterOptions + '</select>' +
          '</div>' +
        '</div>' +
      '</form>';

    const m = window.modal({
      title: isEdit ? 'Editar Regra' : 'Nova Regra',
      body: bodyHtml,
      footer: window.saveCancelFooter(),
    });
    m.open();

    const $form = m.$body.find('form[data-form=rule]');
    const $name = $form.find('input[name=name]');

    setTimeout(function () { $name.trigger('focus'); }, 0);

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($name.val() || '').trim();
      if (name.length < 3) { $name.trigger('focus'); return; }
      const payload = {
        name: name,
        accountId: $form.find('select[name=accountId]').val() || null,
        categoryId: $form.find('select[name=categoryId]').val() || null,
        costCenterId: $form.find('select[name=costCenterId]').val() || null,
      };

      const $btn = m.$el.find('[data-act=save]');
      $btn.prop('disabled', true);

      const p = isEdit
        ? window.App.ImportRuleService.update(existing.id, payload)
        : window.App.ImportRuleService.create(payload);

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Regra atualizada' : 'Regra criada', 'success');
        return loadRules();
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast(err && err.message ? err.message : 'Falha ao salvar regra', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=save]', submit);
  }

  // ── Modal: confirm delete ─────────────────────────────────
  function openDeleteModal(target) {
    window.confirmModal({
      title: 'Excluir Regra',
      body: window.modalText('Tem certeza que deseja excluir a regra <strong>' + esc(target.name) +
        '</strong>? Esta ação não pode ser desfeita.'),
      onConfirm: function (m, reEnable) {
        window.App.ImportRuleService.remove(target.id).then(function () {
          m.close();
          window.toast('Regra removida', 'success');
          return loadRules();
        }).catch(function (err) {
          reEnable();
          window.toast(err && err.message ? err.message : 'Falha ao excluir regra', 'error');
        });
      },
    });
  }

  // ── Event delegation on $root ─────────────────────────────
  function bindRoot($root) {
    $root.on('click.import-rules', '[data-act=new]', function () {
      openFormModal(null);
    });
    $root.on('click.import-rules', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const r = findRule(id);
      if (r) openFormModal(r);
    });
    $root.on('click.import-rules', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const r = findRule(id);
      if (r) openDeleteModal(r);
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['import-rules'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      render();
      loadRules();
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.import-rules');
      }
      state = null;
    }
  };
})();

/* feature/import-rules.js — fatia Regras de Nomenclatura. Um arquivo por fatia: domain →
 * application → infrastructure/secondary → infrastructure/primary, cada bloco abaixo é um IIFE
 * independente (comentário original de cada arquivo preservado como separador de seção). */

/* domain — matches a raw transaction description against the person's
 * cached import rules (regras de nomenclatura). Pure, used identically in 3 places: the manual
 * transaction form, the bank-statement import preview and the invoice import preview. */
(function () {
  /* Uppercase + strip diacritics, so "Agua" matches "AGUA" — same comparison the backend applies
   * when rejecting ambiguous rules (ImportRuleService.normalize), kept in sync by hand since the
   * two runtimes can't share code. ̀-ͯ is the Unicode combining-diacritical-marks block
   * that NFD decomposition splits accents into. */
  function normalize(s) {
    return (s || '')
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toUpperCase()
      .trim();
  }

  /* First rule (array order) whose normalized name is a substring of the normalized description,
   * or null. Rules ambiguous with each other are already rejected at creation time (server-side);
   * independent rules that both happen to match the same description resolve to whichever comes
   * first in `rules`. */
  function match(description, rules) {
    if (!description || !rules || !rules.length) return null;
    const normalizedDescription = normalize(description);
    for (let i = 0; i < rules.length; i++) {
      const rule = rules[i];
      if (!rule || !rule.name) continue;
      if (normalizedDescription.indexOf(normalize(rule.name)) >= 0) return rule;
    }
    return null;
  }

  window.Domain = window.Domain || {};
  window.Domain.ImportRuleMatcher = {
    normalize: normalize,
    match: match,
  };
})();

/* _2_application/import-rule-service.js — ImportRule (regra de nomenclatura) use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()           { return repo.list(); }
  function create(data)     { return repo.create(data); }
  function update(id, data) { return repo.update(id, data); }
  function remove(id)       { return repo.remove(id); }

  function listCached()     { return cache.importRules(); }
  function findById(id)     { return cache.findById('importRules', id); }
  function onChange(cb)     { return cache.subscribe('IMPORT_RULE', cb); }

  window.App = window.App || {};
  window.App.ImportRuleService = {
    init: init,
    list: list, listCached: listCached,
    create: create, update: update, remove: remove,
    findById: findById, onChange: onChange,
  };
})();

/* _3_infrastructure/secondary/import-rule-repository.js — HTTP adapter for /accounts/transaction/rules. */
(function () {
  function create(http) {
    return {
      list:   function ()      { return http.get('/accounts/transaction/rules'); },
      create: function (data)  { return http.post('/accounts/transaction/rules', data); },
      update: function (id, d) { return http.patch('/accounts/transaction/rules/' + id, d); },
      remove: function (id)    { return http.delete('/accounts/transaction/rules/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.ImportRuleRepository = { create: create };
})();

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
    return state;
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

    const $header = window.pageHeader({
      title: 'Regras de Nomenclatura',
      actions: window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Regra',
        attrs: 'data-act="new"'
      })
    });

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

    const accountOptions = window.accountOptionsHtml(initial.accountId, { includeEmpty: true, emptyLabel: '— Nenhuma —', activeOnly: false });
    const categoryFieldHtml = window.categoryPickerHtml({
      items: window.flatCategories(null, true, initial.categoryId),
      selectedId: initial.categoryId,
      selectId: ids.category,
      selectAttrs: ' name="categoryId"',
      placeholder: '— Nenhuma —',
      alwaysPlaceholder: true,
    });
    const costCenterOptions = window.optionsHtml(window.App.CacheStore.costCenters(), initial.costCenterId, {
      labelOf: function (c) { return c.description || c.name || ''; },
    });

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
            categoryFieldHtml +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.costCenter + '">Centro de custo (opcional)</label>' +
            '<select id="' + ids.costCenter + '" name="costCenterId">' + costCenterOptions + '</select>' +
          '</div>' +
        '</div>' +
      '</form>';

    window.formModal({
      title: isEdit ? 'Editar Regra' : 'Nova Regra',
      formName: 'rule',
      body: bodyHtml,
      autofocus: 'input[name=name]',
      onSubmit: function ($form) {
        const $name = $form.find('input[name=name]');
        const name = ($name.val() || '').trim();
        if (name.length < 3) { $name.trigger('focus'); return null; }
        const payload = {
          name: name,
          accountId: $form.find('select[name=accountId]').val() || null,
          categoryId: $form.find('select[name=categoryId]').val() || null,
          costCenterId: $form.find('select[name=costCenterId]').val() || null,
        };
        return isEdit
          ? window.App.ImportRuleService.update(existing.id, payload)
          : window.App.ImportRuleService.create(payload);
      },
      success: function () { return isEdit ? 'Regra atualizada' : 'Regra criada'; },
      failure: 'Falha ao salvar regra',
      onDone: loadRules,
    });
  }

  // ── Modal: confirm delete ─────────────────────────────────
  function openDeleteModal(target) {
    window.confirmModal({
      title: 'Excluir Regra',
      body: window.modalText('Tem certeza que deseja excluir a regra <strong>' + esc(target.name) +
        '</strong>? Esta ação não pode ser desfeita.'),
      onConfirm: function (m, reEnable) {
        window.runMutation(window.App.ImportRuleService.remove(target.id), {
          modal: m, success: 'Regra removida', failure: 'Falha ao excluir regra',
          onDone: loadRules, onError: reEnable,
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
  window.Pages['import-rules'] = window.page({
    ns: '.import-rules',
    state: resetState,
    render: render,
    bind: bindRoot,
    onMount: loadRules,
  });
})();

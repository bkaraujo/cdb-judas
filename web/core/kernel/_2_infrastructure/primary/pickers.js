/* _3_infrastructure/primary/pickers.js — Shared picker UI components and related modals. */
(function () {
  // Quick-create (category/tag) precisa criar via o serviço da fatia dona — mas kernel não pode
  // referenciar App.CategoryService/App.TagService por nome (violaria a regra de fatia). Mesmo
  // padrão do Sidebar.configureClosing: kernel expõe um slot vazio, composition-root injeta o
  // provider real depois que a fatia carregou.
  const quickCreateProviders = { category: null, tag: null };
  function configureQuickCreate(kind, provider) { quickCreateProviders[kind] = provider; }

  // Label for a freshly-created category not yet in the cache (SSE lag).
  function quickCategoryLabel(cat) {
    const name = cat.name || cat.description || '';
    if (cat.parentId) {
      const parent = window.categoryById()[cat.parentId];
      if (parent) return parent.name + ' / ' + name;
    }
    return name;
  }

  // Nature is normally fixed by the originating transaction type. When `nature` is null (quick-create
  // not tied to a single row, e.g. import preview header) the modal exposes Tipo as an editable
  // dropdown instead of the disabled label, defaulting to Despesa.
  function openCategoryCreateModal(nature, onCreated) {
    const uniq = Date.now();
    const nameId = 'qcat-name-' + uniq;
    const parentSelId = 'qcat-parent-' + uniq;
    const natureSelId = 'qcat-nature-' + uniq;
    const fixedNature = (nature === 'INCOME' || nature === 'EXPENSE') ? nature : null;
    const natureLabel = function (n) { return n === 'INCOME' ? 'Receita' : 'Despesa'; };

    function rootsFor(n) {
      return window.App.CacheStore.categories().filter(function (c) {
        return String(c.nature || '').toUpperCase() === n && !c.parentId;
      }).slice().sort(window.sortByName);
    }

    function parentOptsFor(n) {
      return '<option value="">— Nenhuma (categoria raiz) —</option>' +
        rootsFor(n).map(function (p) {
          return '<option value="' + esc(p.id) + '">' + esc(p.name) + '</option>';
        }).join('');
    }

    const tipoFieldHtml = fixedNature
      ? '<input type="text" value="' + esc(natureLabel(fixedNature)) + '" disabled />'
      : '<select id="' + natureSelId + '" name="nature">' +
          '<option value="EXPENSE">Despesa</option>' +
          '<option value="INCOME">Receita</option>' +
        '</select>';

    const bodyHtml =
      '<form data-form="qcat" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label">Tipo</label>' +
            tipoFieldHtml +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + nameId + '">Nome</label>' +
            '<input id="' + nameId + '" name="name" type="text" required ' +
              'placeholder="Nome da categoria" />' +
          '</div>' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + parentSelId + '">Categoria Pai (opcional)</label>' +
            '<select id="' + parentSelId + '" name="parentId">' + parentOptsFor(fixedNature || 'EXPENSE') + '</select>' +
          '</div>' +
        '</div>' +
      '</form>';

    const m = window.formModal({
      title: 'Nova Categoria',
      formName: 'qcat',
      body: bodyHtml,
      autofocus: 'input[name=name]',
      onSubmit: function ($form) {
        const name = ($form.find('input[name=name]').val() || '').trim();
        if (!name) { $form.find('input[name=name]').trigger('focus'); return null; }
        const parentId = $form.find('select[name=parentId]').val() || null;
        const effectiveNature = fixedNature || $form.find('select[name=nature]').val() || 'EXPENSE';
        return quickCreateProviders.category.create({ name: name, nature: effectiveNature, parentId: parentId });
      },
      success: 'Categoria criada',
      failure: 'Falha ao criar categoria',
      onDone: function (created) { if (onCreated) onCreated(created); },
    });

    const $form = m.$body.find('form[data-form=qcat]');

    if (!fixedNature) {
      $form.find('select[name=nature]').on('change', function () {
        $form.find('select[name=parentId]').html(parentOptsFor(this.value));
      });
    }
  }

  // On success the created tag (with id) is handed to `onCreated` so the caller can select it.
  function openTagCreateModal(onCreated) {
    const uniq = Date.now();
    const nameId = 'qtag-name-' + uniq;
    const colorId = 'qtag-color-' + uniq;
    const defaultColor = window.PALETTE.swatches[0];

    const bodyHtml =
      '<form data-form="qtag" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            window.colorNameFieldHtml({
              colorId: colorId, nameId: nameId, color: defaultColor, placeholder: 'Ex: mensal, fixo...',
            }) +
          '</div>' +
        '</div>' +
      '</form>';

    const m = window.formModal({
      title: 'Nova Tag',
      formName: 'qtag',
      body: bodyHtml,
      autofocus: 'input[name=name]',
      onSubmit: function ($form) {
        const $name = $form.find('input[name=name]');
        const name = ($name.val() || '').trim();
        if (!name) { $name.trigger('focus'); return null; }
        return quickCreateProviders.tag.create({
          name: name,
          color: $form.find('input[name=color]').val() || defaultColor,
        });
      },
      success: 'Tag criada',
      failure: 'Falha ao criar tag',
      onDone: function (created) { if (onCreated) onCreated(created); },
    });

    window.bindSwatches(m, m.$body.find('input[name=color]'));
  }

  function categoryPickerHtml(opts) {
    opts = opts || {};
    var items = opts.items || [];
    var selectedId = opts.selectedId != null ? String(opts.selectedId) : '';
    var selectId = opts.selectId || '';
    var selectAttrs = opts.selectAttrs || '';
    var placeholder = opts.placeholder || 'Selecione';
    // alwaysPlaceholder: campo opcional onde "sem categoria" é uma escolha legítima, não só o estado
    // de "nada selecionado ainda" — o placeholder some da lista quando true, senão o usuário não
    // consegue voltar a limpar a seleção pelo próprio picker (ex.: regra de nomenclatura, filtro).
    var alwaysPlaceholder = !!opts.alwaysPlaceholder;

    // Build item list with placeholder when no valid selection
    var hasSel = items.some(function (it) { return String(it.value || it.id) === selectedId && selectedId !== ''; });
    var ddItems;
    if (!items.length) {
      ddItems = [{ value: '', label: 'Sem categorias' }];
    } else if (hasSel && !alwaysPlaceholder) {
      ddItems = items.map(function (it) { return { value: String(it.value || it.id), label: it.label }; });
    } else {
      ddItems = [{ value: '', label: placeholder }].concat(
        items.map(function (it) { return { value: String(it.value || it.id), label: it.label }; })
      );
    }

    // Hidden native <select> (source of truth)
    var optionsHtml = ddItems.map(function (it) {
      var sel = String(it.value) === selectedId ? ' selected' : '';
      return '<option value="' + esc(it.value) + '"' + sel + '>' + esc(it.label) + '</option>';
    }).join('');

    return '<select id="' + esc(selectId) + '"' + selectAttrs + ' style="display:none;">' +
        optionsHtml +
      '</select>' +
      window.searchSelectHtml(ddItems, selectedId, selectId + '-dd', {
        pairedSelectId: selectId,
        lazy: opts.lazy !== false,
        floating: !!opts.floating,
        compact: !!opts.compact,
        disabled: !!opts.disabled,
        title: opts.title || ''
      });
  }

  /* ---- <option> builders ----
   * Shared across the form modals that dropdown-select an account/category/cost-center. */

  // Generic "— placeholder — + items" <option> list, keyed by `.id` (labelOf defaults to
  // name/description). Used where a field is optional (no valid default to fall back to).
  function optionsHtml(items, selectedId, opts) {
    opts = opts || {};
    const labelOf = opts.labelOf || function (it) { return it.name || it.description || ''; };
    const out = ['<option value="">' + esc(opts.placeholder || '— Nenhuma —') + '</option>'];
    (items || []).forEach(function (it) {
      const sel = String(it.id) === String(selectedId) ? ' selected' : '';
      out.push('<option value="' + esc(it.id) + '"' + sel + '>' + esc(labelOf(it)) + '</option>');
    });
    return out.join('');
  }

  // Account <option> list. Defaults to active-only (kept accounts: `opts.keepId`, typically the
  // account already saved on the record being edited, so it survives even if inactivated since).
  // `opts.items` overrides the source list entirely (e.g. import preview's server-picked candidates).
  // `opts.includeEmpty` prepends a placeholder (`opts.emptyLabel`, default '— Selecione —').
  function accountOptionsHtml(selectedId, opts) {
    opts = opts || {};
    const keepId = opts.keepId != null ? String(opts.keepId) : null;
    const items = opts.items || window.accountsList().filter(function (a) {
      return opts.activeOnly === false || a.active !== false ||
        String(a.id) === keepId || String(a.id) === String(selectedId);
    });
    const empty = opts.includeEmpty
      ? '<option value="">' + esc(opts.emptyLabel || '— Selecione —') + '</option>'
      : '';
    if (!items.length) return empty || '<option value="">Nenhuma conta disponível</option>';
    return empty + items.map(function (a) {
      const sel = String(a.id) === String(selectedId) ? ' selected' : '';
      return '<option value="' + esc(a.id) + '"' + sel + '>' + esc(a.name) + '</option>';
    }).join('');
  }

  // Cost-center <option> list with the "Variável" fallback: when nothing is selected yet, the
  // cost center whose description/name matches /vari/i is pre-selected (mirrors the backend's own
  // fallback when a transaction/import row arrives without a costCenterId).
  function costCenterOptionsHtml(selectedId) {
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

  // The `flatCategories → [{value,label}]` shape shared by every category dropdown that isn't
  // built through categoryPickerHtml directly: falls back to a single disabled-looking placeholder
  // item when the nature has no categories at all.
  function categoryItemsFor(nature, keepId) {
    const cats = window.flatCategories(nature, true, keepId);
    if (!cats.length) return [{ value: '', label: 'Nenhuma categoria disponível' }];
    return cats.map(function (c) { return { value: String(c.id), label: c.label }; });
  }

  window.quickCategoryLabel = quickCategoryLabel;
  window.openCategoryCreateModal = openCategoryCreateModal;
  window.openTagCreateModal = openTagCreateModal;
  window.configureQuickCreate = configureQuickCreate;
  window.categoryPickerHtml = categoryPickerHtml;
  window.optionsHtml = optionsHtml;
  window.accountOptionsHtml = accountOptionsHtml;
  window.costCenterOptionsHtml = costCenterOptionsHtml;
  window.categoryItemsFor = categoryItemsFor;
  window.PALETTE = {
    swatches: ['#6366F1', '#10B981', '#F43F5E', '#F59E0B', '#38BDF8', '#A78BFA', '#820AD1', '#1C2951'],
    series:   ['#6366F1', '#38BDF8', '#F59E0B', '#10B981', '#F43F5E', '#A78BFA', '#820AD1', '#1C2951'],
  };
})();

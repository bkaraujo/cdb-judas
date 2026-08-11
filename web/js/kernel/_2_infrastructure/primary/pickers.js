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

    const m = window.modal({
      title: 'Nova Categoria',
      body: bodyHtml,
      footer: window.saveCancelFooter({ saveAttrs: 'data-act="qcat-save" type="submit"' }),
    });
    m.open();

    const $form = m.$body.find('form[data-form=qcat]');
    $form.find('input[name=name]').trigger('focus');

    if (!fixedNature) {
      $form.find('select[name=nature]').on('change', function () {
        $form.find('select[name=parentId]').html(parentOptsFor(this.value));
      });
    }

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($form.find('input[name=name]').val() || '').trim();
      if (!name) { $form.find('input[name=name]').trigger('focus'); return; }
      const parentId = $form.find('select[name=parentId]').val() || null;
      const effectiveNature = fixedNature || $form.find('select[name=nature]').val() || 'EXPENSE';

      const $btn = m.$el.find('[data-act=qcat-save]').prop('disabled', true);
      quickCreateProviders.category.create({
        name: name,
        nature: effectiveNature,
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

  const QTAG_DEFAULT_COLOR = '#6366F1';
  const QTAG_DEFAULT_COLORS = [
    '#6366F1', '#10B981', '#F43F5E', '#F59E0B',
    '#38BDF8', '#A78BFA', '#820AD1', '#FB923C',
  ];

  // On success the created tag (with id) is handed to `onCreated` so the caller can select it.
  function openTagCreateModal(onCreated) {
    const uniq = Date.now();
    const nameId = 'qtag-name-' + uniq;
    const colorId = 'qtag-color-' + uniq;

    const swatchesHtml = window.swatchesHtml(QTAG_DEFAULT_COLORS, QTAG_DEFAULT_COLOR);

    const bodyHtml =
      '<form data-form="qtag" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + nameId + '">Nome</label>' +
            '<div style="display:flex;align-items:center;gap:10px;">' +
              '<input id="' + colorId + '" name="color" type="color" value="' + esc(QTAG_DEFAULT_COLOR) + '" ' +
                'style="width:40px;height:40px;border:1px solid var(--border);' +
                'border-radius:50%;padding:0;background:transparent;cursor:pointer;flex-shrink:0;" />' +
              '<input id="' + nameId + '" name="name" type="text" required placeholder="Ex: mensal, fixo..." />' +
            '</div>' +
            '<div data-region="swatches" style="display:flex;gap:6px;margin-top:8px;flex-wrap:wrap;">' +
              swatchesHtml +
            '</div>' +
          '</div>' +
        '</div>' +
      '</form>';

    const m = window.modal({
      title: 'Nova Tag',
      body: bodyHtml,
      footer: window.saveCancelFooter({ saveAttrs: 'data-act="qtag-save" type="submit"' }),
    });
    m.open();

    const $form = m.$body.find('form[data-form=qtag]');
    const $color = $form.find('input[name=color]');
    const $name  = $form.find('input[name=name]');

    window.bindSwatches(m, $color);
    setTimeout(function () { $name.trigger('focus'); }, 0);

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($name.val() || '').trim();
      if (!name) { $name.trigger('focus'); return; }

      const $btn = m.$el.find('[data-act=qtag-save]').prop('disabled', true);
      quickCreateProviders.tag.create({
        name: name,
        color: $color.val() || QTAG_DEFAULT_COLOR,
      }).then(function (created) {
        m.close();
        window.toast('Tag criada', 'success');
        if (onCreated) onCreated(created);
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast((err && err.message) || 'Falha ao criar tag', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=qtag-save]', submit);
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

  window.quickCategoryLabel = quickCategoryLabel;
  window.openCategoryCreateModal = openCategoryCreateModal;
  window.openTagCreateModal = openTagCreateModal;
  window.configureQuickCreate = configureQuickCreate;
  window.categoryPickerHtml = categoryPickerHtml;
})();

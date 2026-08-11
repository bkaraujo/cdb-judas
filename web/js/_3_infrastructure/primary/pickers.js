/* _3_infrastructure/primary/pickers.js — Shared picker UI components and related modals. */
(function () {
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
      window.App.TagService.create({
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

    // Build item list with placeholder when no valid selection
    var hasSel = items.some(function (it) { return String(it.value || it.id) === selectedId && selectedId !== ''; });
    var ddItems;
    if (!items.length) {
      ddItems = [{ value: '', label: 'Sem categorias' }];
    } else if (hasSel) {
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
  window.categoryPickerHtml = categoryPickerHtml;
})();

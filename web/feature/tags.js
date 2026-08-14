/* feature/tags.js — fatia Tags (CRUD em chips). Um arquivo por fatia: application →
 * infrastructure/secondary → infrastructure/primary. Sem domain própria — Domain.Tag é
 * kernel (core/kernel/_0_domain/tag.js), usado pelos widgets genéricos de picker. */

/* application — Tag use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()           { return repo.list(); }
  function create(data)     { return repo.create(data); }
  function update(id, data) { return repo.update(id, data); }
  function remove(id, opts) { return repo.remove(id, opts); }

  function listCached()     { return cache.tags(); }
  function findById(id)     { return cache.findById('tags', id); }
  function onChange(cb)     { return cache.subscribe('TAG', cb); }

  window.App = window.App || {};
  window.App.TagService = {
    init: init,
    list: list, listCached: listCached,
    create: create, update: update, remove: remove,
    findById: findById, onChange: onChange,
  };
})();

/* infrastructure/secondary — HTTP adapter for /tags. */
(function () {
  function create(http) {
    return {
      list:   function ()        { return http.get('/tags'); },
      create: function (data)    { return http.post('/tags', data); },
      update: function (id, d)   { return http.patch('/tags/' + id, d); },
      remove: function (id, opts) { return http.delete('/tags/' + id + window.Infra.HttpClient.deletionQuery(opts)); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.TagRepository = { create: create };
})();

/* infrastructure/primary — page Tags. */
(function () {
  window.Pages = window.Pages || {};

  const DEFAULT_COLOR = '#6366F1';
  const DEFAULT_COLORS = [
    '#6366F1', '#10B981', '#F43F5E', '#F59E0B',
    '#38BDF8', '#A78BFA', '#820AD1', '#FB923C'
  ];

  let state = null;

  function resetState() {
    state = {
      tags: [],
      $root: null,
    };
  }

  // Tags live in the App.CacheStore (hydrated at login, kept fresh via SSE).
  // Pages must read from cache only — never refetch the list here.
  function syncFromCache() {
    state.tags = window.App.CacheStore.tags().slice();
  }


  function findTag(id) { return window.byId(state.tags, id); }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $header = window.pageHeader({
      title: 'Tags',
      actions: window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Tag',
        attrs: 'data-act="new"'
      })
    });

    let $body;
    if (!state.tags.length) {
      $body = $(window.emptyState({
        icon: 'tag',
        title: 'Nenhuma tag cadastrada',
        desc: 'Clique em "Nova Tag" para começar.'
      }));
    } else {
      const sorted = state.tags.slice().sort(window.sortByName);
      const $card = $('<div class="card"></div>');
      const $chips = $(
        '<div data-region="chips" style="display:flex;flex-wrap:wrap;gap:10px;"></div>'
      );
      sorted.forEach(function (t) { $chips.append(renderChip(t)); });
      $card.append($chips);
      $body = $card;
    }

    $root.empty().append($header).append($body);
  }

  function renderChip(t) {
    const hasColor = !!t.color;
    const color = hasColor ? t.color : '';
    // Muted variant when no color set.
    const dotStyle = hasColor
      ? 'background:' + esc(color) + ';'
      : 'background:var(--text-muted);';
    const chipStyle = hasColor
      ? 'border:1px solid var(--border);background:var(--bg-card);'
      : 'border:1px solid var(--border);background:var(--bg-hover);';
    const nameColor = hasColor ? 'var(--text-primary)' : 'var(--text-secondary)';

    const $chip = $(
      '<div class="tag-chip" data-id="' + esc(t.id) + '" ' +
        (hasColor ? 'data-color="' + esc(color) + '" ' : '') +
        'style="display:inline-flex;align-items:center;gap:10px;' +
        'padding:8px 14px;border-radius:40px;' + chipStyle +
        'cursor:default;transition:all var(--transition);">' +
        '<span style="width:10px;height:10px;border-radius:50%;flex-shrink:0;' + dotStyle + '"></span>' +
        '<span style="font-size:13px;font-weight:700;color:' + nameColor + ';">#' + esc(t.name) + '</span>' +
        '<span data-region="row-actions" style="display:none;align-items:center;gap:2px;margin-left:2px;"></span>' +
      '</div>'
    );

    $chip.find('[data-region=row-actions]')
      .append(chipActionBtn('edit',  'Editar',  t.id))
      .append(chipActionBtn('trash', 'Excluir', t.id, true));

    // Hover reveals actions; if colored, tint border/background.
    $chip.on('mouseenter', function () {
      $chip.find('[data-region=row-actions]').css('display', 'inline-flex');
      if (hasColor) {
        $chip.css('border-color', color);
        $chip.css('background', color + '18');
      } else {
        $chip.css('background', 'var(--bg-card)');
      }
    });
    $chip.on('mouseleave', function () {
      $chip.find('[data-region=row-actions]').css('display', 'none');
      if (hasColor) {
        $chip.css('border-color', 'var(--border)');
        $chip.css('background', 'var(--bg-card)');
      } else {
        $chip.css('background', 'var(--bg-hover)');
      }
    });

    return $chip;
  }

  function chipActionBtn(iconName, title, id, danger) {
    const color = danger ? 'var(--expense)' : 'var(--text-secondary)';
    return $(
      '<button type="button" class="icon-btn" title="' + esc(title) + '" ' +
        'data-act="' + esc(iconName) + '" data-id="' + esc(id) + '" ' +
        'style="width:22px;height:22px;color:' + color + ';">' +
        window.icon(iconName, 12) +
      '</button>'
    );
  }

  // ── Modal: create / edit ──────────────────────────────────
  function openFormModal(existing) {
    const isEdit = !!existing;
    const uniq = Date.now();
    const ids = {
      name: 'tag-name-' + uniq,
      color: 'tag-color-' + uniq,
    };
    const initial = {
      name: isEdit ? (existing.name || '') : '',
      color: isEdit ? (existing.color || DEFAULT_COLOR) : DEFAULT_COLOR,
    };

    const swatchesHtml = window.swatchesHtml(DEFAULT_COLORS, initial.color);

    const bodyHtml =
      '<form data-form="tag" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.name + '">Nome</label>' +
            '<div style="display:flex;align-items:center;gap:10px;">' +
              '<input id="' + ids.color + '" name="color" type="color" ' +
                'value="' + esc(initial.color) + '" ' +
                'style="width:40px;height:40px;border:1px solid var(--border);' +
                'border-radius:50%;padding:0;background:transparent;cursor:pointer;flex-shrink:0;" />' +
              '<input id="' + ids.name + '" name="name" type="text" required ' +
                'placeholder="Ex: mensal, fixo..." value="' + esc(initial.name) + '" />' +
            '</div>' +
            '<div data-region="swatches" style="display:flex;gap:6px;margin-top:8px;flex-wrap:wrap;">' +
              swatchesHtml +
            '</div>' +
          '</div>' +
        '</div>' +
      '</form>';

    const m = window.modal({
      title: isEdit ? 'Editar Tag' : 'Nova Tag',
      body: bodyHtml,
      footer: window.saveCancelFooter(),
    });
    m.open();

    const $form = m.$body.find('form[data-form=tag]');
    const $color = $form.find('input[name=color]');
    const $name  = $form.find('input[name=name]');

    window.bindSwatches(m, $color);

    setTimeout(function () { $name.trigger('focus'); }, 0);

    function submit(e) {
      if (e) e.preventDefault();
      const name = ($name.val() || '').trim();
      if (!name) { $name.trigger('focus'); return; }
      const payload = {
        name: name,
        color: $color.val() || DEFAULT_COLOR,
      };

      const $btn = m.$el.find('[data-act=save]');
      $btn.prop('disabled', true);

      const p = isEdit
        ? window.App.TagService.update(existing.id, payload)
        : window.App.TagService.create(payload);

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Tag atualizada' : 'Tag criada', 'success');
        // SSE UPSERT will refresh CacheStore → TagService.onChange → re-render.
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast(err && err.message ? err.message : 'Falha ao salvar tag', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=save]', submit);
  }

  // ── Modal: confirm delete ─────────────────────────────────
  function openDeleteModal(target) {
    const nameHtml = '<strong>#' + esc(target.name) + '</strong>';
    window.confirmModal({
      title: 'Excluir Tag',
      body: window.modalText('Tem certeza que deseja excluir a tag ' + nameHtml +
        '? As transações vinculadas permanecem — apenas perdem a marcação. Esta ação não pode ser desfeita.'),
      onConfirm: function (m, reEnable) {
        window.App.TagService.remove(target.id).then(function () {
          m.close();
          window.toast('Tag removida', 'success');
          // SSE DELETE will refresh CacheStore → TagService.onChange → re-render.
        }).catch(function (err) {
          if (err && err.status === 409 && err.code === 'LINKED_TRANSACTIONS') {
            m.close();
            openLinkedTagDialog(target, err.count);
            return;
          }
          reEnable();
          window.toast(err && err.message ? err.message : 'Falha ao excluir tag', 'error');
        });
      },
    });
  }

  function openLinkedTagDialog(target, count) {
    const otherTags = state.tags.filter(function (t) { return String(t.id) !== String(target.id); });
    const options = [
      {
        value: 'MOVE', label: 'Mover para outra tag',
        hint: 'As transações desta tag passam a usar a tag escolhida.',
        choices: otherTags.map(function (t) { return { value: t.id, label: '#' + t.name }; }),
      },
      { value: 'DETACH', label: 'Apenas desvincular', hint: 'Remove só a marcação; as transações permanecem intactas.' },
    ];

    window.linkedDeleteDialog({
      title: 'Tag com transações vinculadas',
      intro: 'A tag <strong>#' + esc(target.name) + '</strong> tem ' + count + ' transaç' +
        (count === 1 ? 'ão vinculada' : 'ões vinculadas') + '. Escolha o que fazer:',
      options: options,
      onConfirm: function (choice, m, reEnable) {
        window.App.TagService.remove(target.id, { strategy: choice.strategy, targetId: choice.targetId }).then(function () {
          m.close();
          window.toast('Tag removida', 'success');
        }).catch(function (err) {
          reEnable();
          window.toast((err && err.message) || 'Falha ao excluir tag', 'error');
        });
      },
    });
  }

  // ── Event delegation on $root ─────────────────────────────
  function bindRoot($root) {
    $root.on('click.tags', '[data-act=new]', function () {
      openFormModal(null);
    });
    $root.on('click.tags', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const t = findTag(id);
      if (t) openFormModal(t);
    });
    $root.on('click.tags', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const t = findTag(id);
      if (t) openDeleteModal(t);
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['tags'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      syncFromCache();
      render();
      state.unsubscribe = window.App.TagService.onChange(function () {
        syncFromCache();
        render();
      });
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.tags');
      }
      if (state && state.unsubscribe) state.unsubscribe();
      state = null;
    }
  };
})();

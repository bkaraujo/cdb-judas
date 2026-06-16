/* pages/categories.js — Categorias (CRUD com árvore pai/filho por natureza). */
(function () {
  window.Pages = window.Pages || {};

  const NATURE_EXPENSE = 'EXPENSE';
  const NATURE_REVENUE = 'INCOME';

  // ── State (per mount, captured in closure) ────────────────
  let state = null;

  function resetState() {
    state = {
      tab: 'expense',           // 'expense' | 'income'
      categories: [],
      expanded: {},
      $root: null,
    };
  }

  // Categories live in the App.CacheStore (hydrated at login, SSE-refreshed).
  function syncFromCache() {
    state.categories = window.App.CacheStore.categories().slice();
  }

  // ── Toast (transient div bottom-right) ────────────────────

  // ── Data helpers ──────────────────────────────────────────
  function currentNature() {
    return state.tab === 'expense' ? NATURE_EXPENSE : NATURE_REVENUE;
  }


  function natureRoots(nature) {
    return state.categories
      .filter(function (c) { return c.nature === nature && !c.parentId; })
      .slice()
      .sort(window.sortByName);
  }

  function childrenOf(parentId) {
    return state.categories
      .filter(function (c) { return c.parentId === parentId; })
      .slice()
      .sort(window.sortByName);
  }

  function findCategory(id) { return window.byId(state.categories, id); }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const expenseCount = state.categories.filter(function (c) {
      return c.nature === NATURE_EXPENSE && !c.parentId;
    }).length;
    const incomeCount = state.categories.filter(function (c) {
      return c.nature === NATURE_REVENUE && !c.parentId;
    }).length;

    const tab = state.tab;
    const tree = natureRoots(currentNature()).map(function (r) {
      return { root: r, children: childrenOf(r.id) };
    });

    // Page header
    const $header = $(
      '<div class="page-header">' +
        '<h1>Categorias</h1>' +
        '<div class="page-header-actions" data-region="actions"></div>' +
      '</div>'
    );
    $header.find('[data-region=actions]').append(
      window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Categoria',
        attrs: 'data-act="new"'
      })
    );

    // Type sidebar (left)
    const $typeNav = $(
      '<div class="card" style="padding:16px;align-self:start;">' +
        '<div style="display:flex;flex-direction:column;gap:4px;">' +
          tabButton('expense', 'Despesas', 'arrowDown', expenseCount, tab === 'expense') +
          tabButton('income',  'Receitas', 'arrowUp',   incomeCount,  tab === 'income') +
        '</div>' +
      '</div>'
    );

    // Right column: list
    let $list;
    if (tree.length === 0) {
      $list = $(
        '<div class="card" style="padding:40px 20px;text-align:center;color:var(--text-muted);font-size:13px;">' +
          'Nenhuma categoria cadastrada.' +
        '</div>'
      );
    } else {
      $list = $('<div style="display:flex;flex-direction:column;gap:12px;"></div>');
      tree.forEach(function (node) {
        $list.append(renderRootCard(node.root, node.children));
      });
    }

    // Layout container
    const $grid = $(
      '<div style="display:grid;grid-template-columns:240px 1fr;gap:16px;align-items:start;"></div>'
    );
    $grid.append($typeNav).append($list);

    $root.empty().append($header).append($grid);
  }

  function tabButton(id, label, iconName, count, active) {
    const color = id === 'expense' ? 'expense' : 'income';
    const bg = active ? 'var(--' + color + '-light)' : 'transparent';
    const fg = active ? 'var(--' + color + ')'      : 'var(--text-secondary)';
    return (
      '<button type="button" data-act="set-tab" data-tab="' + esc(id) + '" ' +
        'style="display:flex;align-items:center;justify-content:space-between;' +
        'padding:10px 12px;border-radius:var(--radius-sm);' +
        'background:' + bg + ';color:' + fg + ';' +
        'border:none;cursor:pointer;font-weight:600;font-size:13px;' +
        'transition:all var(--transition);">' +
        '<span style="display:flex;align-items:center;gap:8px;">' +
          window.icon(iconName, 15) + esc(label) +
        '</span>' +
        window.badge(String(count), color) +
      '</button>'
    );
  }

  function renderRootCard(root, children) {
    const hasChildren = children.length > 0;
    const isExpanded = !!state.expanded[root.id];
    const showChildren = hasChildren && isExpanded;

    const $card = $(
      '<div class="card" style="padding:0;overflow:hidden;"></div>'
    );

    const chevronHtml = hasChildren
      ? window.icon(isExpanded ? 'chevronDown' : 'chevronRight', 14)
      : '<span style="display:inline-block;width:14px;"></span>';
    const rootAttrs = hasChildren
      ? ' data-act="toggle" data-id="' + esc(root.id) + '" style="cursor:pointer;'
      : ' style="cursor:default;';
    const $rootRow = $(
      '<div data-row="root" data-id="' + esc(root.id) + '"' + rootAttrs +
        'display:flex;align-items:center;justify-content:space-between;' +
        'padding:13px 20px;border-bottom:' +
        (showChildren ? '1px solid var(--border-light)' : 'none') + ';' +
        'transition:background var(--transition);">' +
        '<div style="display:flex;align-items:center;gap:10px;min-width:0;">' +
          '<span style="display:inline-flex;align-items:center;color:var(--text-muted);">' +
            chevronHtml +
          '</span>' +
          '<span style="font-size:13px;font-weight:600;color:var(--text-primary);' +
          'text-transform:uppercase;letter-spacing:0.02em;">' + esc(root.name) + '</span>' +
          (hasChildren ? window.badge(String(children.length), 'muted') : '') +
        '</div>' +
        '<div data-region="row-actions" style="display:flex;gap:2px;"></div>' +
      '</div>'
    );
    if (!root.isSystem) {
      $rootRow.find('[data-region=row-actions]')
        .append(window.rowActionBtn('edit',  'Editar',  root.id))
        .append(window.rowActionBtn('trash', 'Excluir', root.id, true));
    }
    $rootRow.on('mouseenter', function () { $(this).css('background', 'var(--bg-hover)'); });
    $rootRow.on('mouseleave', function () { $(this).css('background', 'transparent'); });
    $card.append($rootRow);

    if (showChildren) {
      children.forEach(function (sub, idx) {
        const isLast = idx === children.length - 1;
        const $subRow = $(
          '<div data-row="sub" data-id="' + esc(sub.id) + '" ' +
            'style="display:flex;align-items:center;justify-content:space-between;' +
            'padding:10px 20px 10px 44px;' +
            (isLast ? '' : 'border-bottom:1px solid var(--border-light);') +
            'transition:background var(--transition);">' +
            '<div style="display:flex;align-items:center;gap:8px;min-width:0;">' +
              '<span style="color:var(--text-muted);font-family:monospace;font-size:12px;">└─</span>' +
              '<span style="font-size:13px;color:var(--text-secondary);">' + esc(sub.name) + '</span>' +
            '</div>' +
            '<div data-region="row-actions" style="display:flex;gap:2px;"></div>' +
          '</div>'
        );
        if (!sub.isSystem) {
          $subRow.find('[data-region=row-actions]')
            .append(window.rowActionBtn('edit',  'Editar',  sub.id))
            .append(window.rowActionBtn('trash', 'Excluir', sub.id, true));
        }
        $subRow.on('mouseenter', function () { $(this).css('background', 'var(--bg-hover)'); });
        $subRow.on('mouseleave', function () { $(this).css('background', 'transparent'); });
        $card.append($subRow);
      });
    }

    return $card;
  }


  // ── Modal: create / edit ──────────────────────────────────
  function openFormModal(existing) {
    const isEdit = !!existing;
    const initialNature = isEdit ? existing.nature : currentNature();
    const name = isEdit ? existing.name : '';
    const parentId = isEdit ? (existing.parentId || '') : '';

    const nameId = 'cat-name-' + Date.now();
    const parentSelectId = 'cat-parent-' + Date.now();
    const natureName = 'cat-nature-' + Date.now();

    const showParent = !isEdit || existing.parentId; // hide for root edits (matches nextjs)
    const showNature = !isEdit;                       // nature fixed on edit

    function parentOptionsHtml(nature) {
      const parents = natureRoots(nature).filter(function (c) {
        return !existing || c.id !== existing.id;
      });
      const empty = isEdit ? '' : '<option value="">— Nenhum (raiz) —</option>';
      return empty + parents.map(function (p) {
        const sel = p.id === parentId ? ' selected' : '';
        return '<option value="' + esc(p.id) + '"' + sel + '>' + esc(p.name) + '</option>';
      }).join('');
    }

    const natureHtml = showNature
      ? '<div class="form-group full">' +
          '<label class="form-label">Tipo</label>' +
          '<div style="display:flex;gap:16px;align-items:center;">' +
            '<label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px;">' +
              '<input type="radio" name="' + natureName + '" value="' + NATURE_EXPENSE + '"' +
                (initialNature === NATURE_EXPENSE ? ' checked' : '') + '> Despesa' +
            '</label>' +
            '<label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px;">' +
              '<input type="radio" name="' + natureName + '" value="' + NATURE_REVENUE + '"' +
                (initialNature === NATURE_REVENUE ? ' checked' : '') + '> Receita' +
            '</label>' +
          '</div>' +
        '</div>'
      : '';

    const bodyHtml =
      '<form data-form="cat" autocomplete="off">' +
        '<div class="form-grid">' +
          natureHtml +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + nameId + '">Nome</label>' +
            '<input id="' + nameId + '" name="name" type="text" required ' +
              'placeholder="Nome da categoria" value="' + esc(name) + '" />' +
          '</div>' +
          (showParent
            ? '<div class="form-group full">' +
                '<label class="form-label" for="' + parentSelectId + '">' +
                  (isEdit ? 'Categoria Pai' : 'Categoria Pai (opcional)') +
                '</label>' +
                '<select id="' + parentSelectId + '" name="parentId">' +
                  parentOptionsHtml(initialNature) +
                '</select>' +
              '</div>'
            : ''
          ) +
        '</div>' +
      '</form>';

    const m = window.modal({
      title: isEdit ? 'Editar Categoria' : 'Nova Categoria',
      body: bodyHtml,
      footer: window.saveCancelFooter(),
    });

    m.open();

    const $form = m.$body.find('form[data-form=cat]');

    // Refresh parent options when nature radio changes (new mode only).
    if (showNature && showParent) {
      const $natureInputs = $form.find('input[name="' + natureName + '"]');
      const $parentSelect = $form.find('#' + parentSelectId);
      $natureInputs.on('change click', function () {
        if (!this.checked) return;
        $parentSelect.html(parentOptionsHtml(this.value));
      });
    }

    // Wire save (form submit + button click)
    function submit(e) {
      if (e) e.preventDefault();
      const newName = ($form.find('input[name=name]').val() || '').trim();
      if (!newName) {
        $form.find('input[name=name]').trigger('focus');
        return;
      }
      const newParent = $form.find('select[name=parentId]').val() || null;
      const newNature = isEdit
        ? existing.nature
        : (m.$el.find('input[name="' + natureName + '"]:checked').val() || initialNature);

      // Disable button during request
      const $btn = m.$el.find('[data-act=save]');
      $btn.prop('disabled', true);

      let p;
      if (isEdit) {
        p = window.App.CategoryService.update(existing.id, {
          name: newName,
          parentId: newParent || null,
        });
      } else {
        p = window.App.CategoryService.create({
          name: newName,
          nature: newNature,
          parentId: newParent || null,
        });
      }

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Categoria atualizada' : 'Categoria criada', 'success');
        // SSE UPSERT will refresh CacheStore → CategoryService.onChange → re-render.
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast(err && err.message ? err.message : 'Falha ao salvar categoria');
      });
    }

    $form.on('submit', submit);
    // Footer button is OUTSIDE the form (it lives in modal-footer), wire click → submit.
    m.$el.on('click', '[data-act=save]', submit);
  }

  // ── Modal: confirm delete ─────────────────────────────────
  function openDeleteModal(target) {
    const nameHtml = '<strong>' + esc(target.name) + '</strong>';
    window.confirmModal({
      title: 'Excluir Categoria',
      body: window.modalText('Tem certeza que deseja excluir a categoria ' + nameHtml + '? Esta ação não pode ser desfeita.'),
      onConfirm: function (m, reEnable) {
        window.App.CategoryService.remove(target.id).then(function () {
          m.close();
          window.toast('Categoria excluída', 'success');
          // SSE DELETE will refresh CacheStore → CategoryService.onChange → re-render.
        }).catch(function (err) {
          reEnable();
          window.toast(err && err.message ? err.message : 'Falha ao excluir categoria');
        });
      },
    });
  }

  // ── Event delegation on $root ─────────────────────────────
  function bindRoot($root) {
    $root.on('click.cats', '[data-act=new]', function () {
      openFormModal(null);
    });
    $root.on('click.cats', '[data-act=set-tab]', function () {
      const t = $(this).attr('data-tab');
      if (t && t !== state.tab) {
        state.tab = t;
        render();
      }
    });
    $root.on('click.cats', '[data-act=toggle]', function (e) {
      if ($(e.target).closest('[data-region=row-actions]').length) return;
      const id = $(this).attr('data-id');
      if (!id) return;
      state.expanded[id] = !state.expanded[id];
      render();
    });
    $root.on('click.cats', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const cat = findCategory(id);
      if (cat) openFormModal(cat);
    });
    $root.on('click.cats', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const cat = findCategory(id);
      if (cat) openDeleteModal(cat);
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['categories'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      syncFromCache();
      render();
      state.unsubscribe = window.App.CategoryService.onChange(function () {
        syncFromCache();
        render();
      });
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.cats');
      }
      if (state && state.unsubscribe) state.unsubscribe();
      state = null;
    }
  };
})();

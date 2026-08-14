/* feature/categories.js — fatia Categorias (CRUD com árvore pai/filho por natureza). Um
 * arquivo por fatia: application → infrastructure/secondary → infrastructure/primary, cada
 * bloco abaixo é um IIFE independente (comentário original de cada arquivo preservado como
 * separador de seção). Sem domain própria — Domain.Category é kernel (core/kernel/_0_domain/
 * category.js), usado pelos widgets genéricos de picker. */

/* application — Category use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()           { return repo.list(); }
  function create(data)     { return repo.create(data); }
  function update(id, data) { return repo.update(id, data); }
  function remove(id, opts) { return repo.remove(id, opts); }

  function listCached()     { return cache.categories(); }
  function findById(id)     { return cache.findById('categories', id); }

  function rootsByNature(nature) { return window.Domain.Category.rootsByNature(cache.categories(), nature); }
  function childrenOf(parentId)  { return window.Domain.Category.childrenOf(cache.categories(), parentId); }
  function labelChain(id)        { return window.Domain.Category.labelChain(cache.categories(), id); }
  function eligibleParents(nature, excludeId) {
    return window.Domain.Category.eligibleParents(cache.categories(), nature, excludeId);
  }
  function isEffectivelyActive(id) { return window.Domain.Category.isEffectivelyActive(cache.categories(), id); }

  function onChange(cb) { return cache.subscribe('CATEGORY', cb); }

  window.App = window.App || {};
  window.App.CategoryService = {
    init: init,
    list: list,
    listCached: listCached,
    create: create,
    update: update,
    remove: remove,
    findById: findById,
    rootsByNature: rootsByNature,
    childrenOf: childrenOf,
    labelChain: labelChain,
    eligibleParents: eligibleParents,
    isEffectivelyActive: isEffectivelyActive,
    onChange: onChange,
  };
})();

/* _3_infrastructure/secondary/category-repository.js — HTTP adapter for /categories. */
(function () {
  function create(http) {
    return {
      list:   function ()        { return http.get('/categories'); },
      create: function (data)    { return http.post('/categories', data); },
      update: function (id, d)   { return http.patch('/categories/' + id, d); },
      remove: function (id, opts) { return http.delete('/categories/' + id + window.Infra.HttpClient.deletionQuery(opts)); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.CategoryRepository = { create: create };
})();

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

  function reactivateBtn(id) {
    return window.rowActionBtn('eye', 'Reativar', id, { act: 'reactivate' });
  }

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
    const $header = window.pageHeader({
      title: 'Categorias',
      actions: window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Nova Categoria',
        attrs: 'data-act="new"'
      })
    });

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
    const rootActive = root.active !== false;
    const $rootRow = $(
      '<div data-row="root" data-id="' + esc(root.id) + '"' + rootAttrs +
        'display:flex;align-items:center;justify-content:space-between;' +
        'padding:13px 20px;border-bottom:' +
        (showChildren ? '1px solid var(--border-light)' : 'none') + ';' +
        (rootActive ? '' : 'opacity:0.55;') +
        'transition:background var(--transition);">' +
        '<div style="display:flex;align-items:center;gap:10px;min-width:0;">' +
          '<span style="display:inline-flex;align-items:center;color:var(--text-muted);">' +
            chevronHtml +
          '</span>' +
          '<span style="font-size:13px;font-weight:600;color:var(--text-primary);' +
          'text-transform:uppercase;letter-spacing:0.02em;">' + esc(root.name) + '</span>' +
          (hasChildren ? window.badge(String(children.length), 'muted') : '') +
          (rootActive ? '' : window.badge('Inativa', 'muted')) +
        '</div>' +
        '<div data-region="row-actions" style="display:flex;gap:2px;"></div>' +
      '</div>'
    );
    if (!root.isSystem) {
      const $rootActions = $rootRow.find('[data-region=row-actions]');
      if (rootActive) {
        $rootActions
          .append(window.rowActionBtn('edit',  'Editar',  root.id))
          .append(window.rowActionBtn('trash', 'Excluir', root.id, true));
      } else {
        $rootActions.append(reactivateBtn(root.id));
      }
    }
    $rootRow.on('mouseenter', function () { $(this).css('background', 'var(--bg-hover)'); });
    $rootRow.on('mouseleave', function () { $(this).css('background', 'transparent'); });
    $card.append($rootRow);

    if (showChildren) {
      children.forEach(function (sub, idx) {
        const isLast = idx === children.length - 1;
        const subActive = sub.active !== false;
        const $subRow = $(
          '<div data-row="sub" data-id="' + esc(sub.id) + '" ' +
            'style="display:flex;align-items:center;justify-content:space-between;' +
            'padding:10px 20px 10px 44px;' +
            (isLast ? '' : 'border-bottom:1px solid var(--border-light);') +
            (subActive ? '' : 'opacity:0.55;') +
            'transition:background var(--transition);">' +
            '<div style="display:flex;align-items:center;gap:8px;min-width:0;">' +
              '<span style="color:var(--text-muted);font-family:monospace;font-size:12px;">└─</span>' +
              '<span style="font-size:13px;color:var(--text-secondary);">' + esc(sub.name) + '</span>' +
              (subActive ? '' : window.badge('Inativa', 'muted')) +
            '</div>' +
            '<div data-region="row-actions" style="display:flex;gap:2px;"></div>' +
          '</div>'
        );
        if (!sub.isSystem) {
          const $subActions = $subRow.find('[data-region=row-actions]');
          if (subActive) {
            $subActions
              .append(window.rowActionBtn('edit',  'Editar',  sub.id))
              .append(window.rowActionBtn('trash', 'Excluir', sub.id, true));
          } else {
            $subActions.append(reactivateBtn(sub.id));
          }
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

    const m = window.formModal({
      title: isEdit ? 'Editar Categoria' : 'Nova Categoria',
      formName: 'cat',
      body: bodyHtml,
      onSubmit: function ($form) {
        const newName = ($form.find('input[name=name]').val() || '').trim();
        if (!newName) {
          $form.find('input[name=name]').trigger('focus');
          return null;
        }
        const newParent = $form.find('select[name=parentId]').val() || null;
        const newNature = isEdit
          ? existing.nature
          : (m.$el.find('input[name="' + natureName + '"]:checked').val() || initialNature);

        return isEdit
          ? window.App.CategoryService.update(existing.id, { name: newName, parentId: newParent || null })
          : window.App.CategoryService.create({ name: newName, nature: newNature, parentId: newParent || null });
      },
      success: function () { return isEdit ? 'Categoria atualizada' : 'Categoria criada'; },
      failure: 'Falha ao salvar categoria',
      // SSE UPSERT will refresh CacheStore → CategoryService.onChange → re-render.
    });

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
  }

  // ── Modal: confirm delete ─────────────────────────────────
  function subtreeIds(target) {
    const ids = [target.id];
    if (!target.parentId) childrenOf(target.id).forEach(function (c) { ids.push(c.id); });
    return ids;
  }

  function openDeleteModal(target) {
    const nameHtml = '<strong>' + esc(target.name) + '</strong>';
    window.deleteWithLinkedFallback({
      title: 'Excluir Categoria',
      body: window.modalText('Tem certeza que deseja excluir a categoria ' + nameHtml + '? Esta ação não pode ser desfeita.'),
      remove: function () { return window.App.CategoryService.remove(target.id); },
      success: 'Categoria excluída',
      failure: 'Falha ao excluir categoria',
      // SSE DELETE will refresh CacheStore → CategoryService.onChange → re-render.
      linked: {
        title: 'Categoria com transações vinculadas',
        intro: function (count) {
          const hasChildren = !target.parentId && childrenOf(target.id).length > 0;
          return 'A categoria <strong>' + esc(target.name) + '</strong>' +
            (hasChildren ? ' (e suas subcategorias)' : '') + ' tem ' + window.pluralTransactions(count) + '. Escolha o que fazer:';
        },
        options: function (count) {
          const excluded = subtreeIds(target);
          const hasChildren = !target.parentId && childrenOf(target.id).length > 0;
          const eligibleTargets = state.categories.filter(function (c) {
            return !!c.parentId
              && c.nature === target.nature
              && excluded.indexOf(c.id) === -1
              && window.Domain.Category.isEffectivelyActive(state.categories, c.id);
          });
          return [
            {
              value: 'MOVE', label: 'Mover para outra subcategoria',
              hint: 'As transações' + (hasChildren ? ' desta categoria e das subcategorias' : '') + ' passam para a categoria escolhida.',
              choices: eligibleTargets.map(function (c) { return { value: c.id, label: window.App.CategoryService.labelChain(c.id) }; }),
            },
            {
              value: 'DELETE', label: 'Excluir transações', danger: true,
              hint: 'Apaga a categoria' + (hasChildren ? ' (e subcategorias)' : '') + ' e ' + window.pluralTransactions(count) + '.',
            },
            { value: 'INATIVAR', label: 'Inativar categoria', hint: 'Some dos lançamentos novos; o histórico é mantido.' },
          ];
        },
        dispatch: function (choice) {
          if (choice.strategy === 'INATIVAR') {
            return window.App.CategoryService.update(target.id, { name: target.name, parentId: target.parentId || null, active: false });
          }
          return window.App.CategoryService.remove(target.id, { strategy: choice.strategy, targetId: choice.targetId });
        },
        success: function (choice) { return choice.strategy === 'INATIVAR' ? 'Categoria inativada' : 'Categoria excluída'; },
        failure: 'Falha ao excluir categoria',
      },
    });
  }

  function reactivateCategory(cat) {
    window.runMutation(
      window.App.CategoryService.update(cat.id, { name: cat.name, parentId: cat.parentId || null, active: true }),
      { success: 'Categoria reativada', failure: 'Falha ao reativar categoria' }
    );
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
    $root.on('click.cats', '[data-act=reactivate]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const cat = findCategory(id);
      if (cat) reactivateCategory(cat);
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

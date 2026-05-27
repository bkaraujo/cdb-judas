/* pages/cost-centers.js — Centros de Custo (CRUD simples em card-list). */
(function () {
  window.Pages = window.Pages || {};

  let state = null;

  function resetState() {
    state = {
      costCenters: [],
      $root: null,
    };
  }

  // Cost centers live in the App.CacheStore (hydrated at login, SSE-refreshed).
  function syncFromCache() {
    state.costCenters = window.App.CacheStore.costCenters().slice();
  }

  // ── Helpers ───────────────────────────────────────────────
  function sortByDescription(a, b) {
    const an = String(a.description || '').toLowerCase();
    const bn = String(b.description || '').toLowerCase();
    return an.localeCompare(bn, 'pt-BR');
  }

  function findCC(id) {
    for (let i = 0; i < state.costCenters.length; i++) {
      if (String(state.costCenters[i].id) === String(id)) return state.costCenters[i];
    }
    return null;
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $header = $(
      '<div class="page-header">' +
        '<h1>Centros de Custo</h1>' +
        '<div class="page-header-actions" data-region="actions"></div>' +
      '</div>'
    );
    $header.find('[data-region=actions]').append(
      window.btn({
        variant: 'primary', size: 'md', icon: 'plus', label: 'Novo Centro',
        attrs: 'data-act="new"'
      })
    );

    let $body;
    if (!state.costCenters.length) {
      $body = $(window.emptyState({
        icon: 'briefcase',
        title: 'Nenhum centro de custo cadastrado',
        desc: 'Clique em "Novo Centro" para começar.'
      }));
    } else {
      const sorted = state.costCenters.slice().sort(sortByDescription);
      $body = $('<div class="card card-list"></div>');
      sorted.forEach(function (cc) { $body.append(renderRow(cc)); });
    }

    $root.empty().append($header).append($body);
  }

  function renderRow(cc) {
    const color = cc.color || '';
    const hasColor = !!color;
    const description = cc.description || '';
    const name = cc.name || '';
    const subtitle = (name && description && name !== description) ? description : '';
    const title = name || description;

    const avatarHtml = hasColor
      ? '<span style="width:12px;height:12px;border-radius:50%;flex-shrink:0;background:' + esc(color) + ';"></span>'
      : '<div style="width:36px;height:36px;border-radius:8px;flex-shrink:0;' +
          'background:var(--accent-light);color:var(--accent);' +
          'display:flex;align-items:center;justify-content:center;">' +
          window.icon('briefcase', 16) +
        '</div>';

    const $row = $(
      '<div class="card-row" data-id="' + esc(cc.id) + '">' +
        '<div class="card-row-main">' +
          avatarHtml +
          '<div style="min-width:0;display:flex;flex-direction:column;">' +
            '<span class="row-title" style="color:var(--text-primary);' +
              'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + esc(title) + '</span>' +
            (subtitle
              ? '<span class="row-sub" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' +
                  esc(subtitle) +
                '</span>'
              : '') +
          '</div>' +
        '</div>' +
        '<div class="card-row-actions" data-region="row-actions"></div>' +
      '</div>'
    );

    $row.find('[data-region=row-actions]')
      .append(window.rowActionBtn('edit',  'Editar',  cc.id))
      .append(window.rowActionBtn('trash', 'Excluir', cc.id, true));

    return $row;
  }


  // ── Modal: create / edit ──────────────────────────────────
  function openFormModal(existing) {
    const isEdit = !!existing;
    const uniq = Date.now();
    const ids = {
      description: 'cc-desc-' + uniq,
    };
    const initial = {
      description: isEdit ? (existing.description || '') : '',
    };

    const bodyHtml =
      '<form data-form="cc" autocomplete="off">' +
        '<div class="form-grid">' +
          '<div class="form-group full">' +
            '<label class="form-label" for="' + ids.description + '">Nome</label>' +
            '<input id="' + ids.description + '" name="description" type="text" required ' +
              'placeholder="Ex: Fixo, Variável, Casa..." value="' + esc(initial.description) + '" />' +
          '</div>' +
        '</div>' +
      '</form>';

    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $save = window.btn({
      variant: 'primary', size: 'md', label: 'Salvar',
      attrs: 'data-act="save" type="submit"'
    });
    const $footer = $('<div style="display:flex;gap:10px;"></div>').append($cancel).append($save);

    const m = window.modal({
      title: isEdit ? 'Editar Centro de Custo' : 'Novo Centro de Custo',
      body: bodyHtml,
      footer: $footer,
    });
    m.open();

    const $form = m.$body.find('form[data-form=cc]');
    const $desc = $form.find('input[name=description]');

    setTimeout(function () { $desc.trigger('focus'); }, 0);

    function submit(e) {
      if (e) e.preventDefault();
      const description = ($desc.val() || '').trim();
      if (!description) { $desc.trigger('focus'); return; }
      const payload = { description: description };

      const $btn = m.$el.find('[data-act=save]');
      $btn.prop('disabled', true);

      const p = isEdit
        ? window.App.CostCenterService.update(existing.id, payload)
        : window.App.CostCenterService.create(payload);

      p.then(function () {
        m.close();
        window.toast(isEdit ? 'Centro de custo atualizado' : 'Centro de custo criado', 'success');
        // SSE UPSERT will refresh CacheStore → CostCenterService.onChange → re-render.
      }).catch(function (err) {
        $btn.prop('disabled', false);
        window.toast(err && err.message ? err.message : 'Falha ao salvar centro de custo', 'error');
      });
    }

    $form.on('submit', submit);
    m.$el.on('click', '[data-act=save]', submit);
  }

  // ── Modal: confirm delete ─────────────────────────────────
  function openDeleteModal(target) {
    const label = target.description || target.name || '';
    const nameHtml = '<strong>' + esc(label) + '</strong>';
    const bodyHtml =
      '<p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">' +
        'Tem certeza que deseja excluir o centro de custo ' + nameHtml + '? ' +
        'Esta ação não pode ser desfeita.' +
      '</p>';

    const $cancel = window.btn({
      variant: 'secondary', size: 'md', label: 'Cancelar',
      attrs: 'data-modal-close="1" type="button"'
    });
    const $confirm = window.btn({
      variant: 'danger', size: 'md', icon: 'trash', label: 'Excluir',
      attrs: 'data-act="confirm-delete" type="button"'
    });
    const $footer = $('<div style="display:flex;gap:10px;"></div>').append($cancel).append($confirm);

    const m = window.modal({
      title: 'Excluir Centro de Custo',
      body: bodyHtml,
      footer: $footer,
    });
    m.open();

    m.$el.on('click', '[data-act=confirm-delete]', function () {
      const $b = $(this).prop('disabled', true);
      window.App.CostCenterService.remove(target.id).then(function () {
        m.close();
        window.toast('Centro de custo excluído', 'success');
        // SSE DELETE will refresh CacheStore → CostCenterService.onChange → re-render.
      }).catch(function (err) {
        $b.prop('disabled', false);
        window.toast(err && err.message ? err.message : 'Falha ao excluir centro de custo', 'error');
      });
    });
  }

  // ── Event delegation on $root ─────────────────────────────
  function bindRoot($root) {
    $root.on('click.ccs', '[data-act=new]', function () {
      openFormModal(null);
    });
    $root.on('click.ccs', '[data-act=edit]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const cc = findCC(id);
      if (cc) openFormModal(cc);
    });
    $root.on('click.ccs', '[data-act=trash]', function (e) {
      e.stopPropagation();
      const id = $(this).attr('data-id');
      const cc = findCC(id);
      if (cc) openDeleteModal(cc);
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['cost-centers'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      bindRoot($root);
      syncFromCache();
      render();
      state.unsubscribe = window.App.CostCenterService.onChange(function () {
        syncFromCache();
        render();
      });
    },
    unmount: function () {
      if (state && state.$root) {
        state.$root.off('.ccs');
      }
      if (state && state.unsubscribe) state.unsubscribe();
      state = null;
    }
  };
})();

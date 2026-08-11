/* pages/cost-centers.js — Centros de Custo (dado fixo do sistema, somente leitura). */
(function () {
  window.Pages = window.Pages || {};

  let state = null;

  function resetState() {
    state = {
      costCenters: [],
      $root: null,
    };
  }

  // Cost centers live in the App.CacheStore (hydrated at login from the global catalog).
  function syncFromCache() {
    state.costCenters = window.App.CacheStore.costCenters().slice();
  }

  function sortByDescription(a, b) {
    const an = String(a.description || '').toLowerCase();
    const bn = String(b.description || '').toLowerCase();
    return an.localeCompare(bn, 'pt-BR');
  }

  // ── Render ────────────────────────────────────────────────
  function render() {
    const $root = state.$root;
    if (!$root) return;

    const $header = $(
      '<div class="page-header">' +
        '<h1>Centros de Custo</h1>' +
      '</div>'
    );

    let $body;
    if (!state.costCenters.length) {
      $body = $(window.emptyState({
        icon: 'briefcase',
        title: 'Nenhum centro de custo disponível',
        desc: 'Os centros de custo são fixos do sistema.'
      }));
    } else {
      const sorted = state.costCenters.slice().sort(sortByDescription);
      $body = $('<div class="card card-list"></div>');
      sorted.forEach(function (cc) { $body.append(renderRow(cc)); });
    }

    $root.empty().append($header).append($body);
  }

  function renderRow(cc) {
    const description = cc.description || '';
    const name = cc.name || '';
    const subtitle = (name && description && name !== description) ? description : '';
    const title = name || description;

    const avatarHtml =
      '<div style="width:36px;height:36px;border-radius:8px;flex-shrink:0;' +
        'background:var(--accent-light);color:var(--accent);' +
        'display:flex;align-items:center;justify-content:center;">' +
        window.icon('briefcase', 16) +
      '</div>';

    return $(
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
      '</div>'
    );
  }

  // ── Lifecycle ─────────────────────────────────────────────
  window.Pages['cost-centers'] = {
    mount: function ($root) {
      resetState();
      state.$root = $root;
      syncFromCache();
      render();
    },
    unmount: function () {
      state = null;
    }
  };
})();

/* feature/cost-centers.js — fatia Centros de Custo (dado fixo do sistema, somente leitura).
 * Um arquivo por fatia: domain → application → infrastructure/secondary → infrastructure/primary,
 * cada bloco preservando o IIFE original (mesma ordem de execução que o barrel antigo garantia). */

/* domain — CostCenter entity. Pure. */
(function () {
  function normalize(raw) {
    if (!raw) return null;
    return {
      id:          raw.id,
      name:        raw.name || '',
      description: raw.description || '',
      color:       raw.color || null,
    };
  }

  /* Show secondary line only when description differs from the name. */
  function displaySubtitle(c) {
    if (!c) return '';
    if (c.name && c.description && c.name !== c.description) return c.description;
    return '';
  }

  window.Domain = window.Domain || {};
  window.Domain.CostCenter = {
    normalize: normalize,
    displaySubtitle: displaySubtitle,
  };
})();

/* application — CostCenter use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  // Centro de custo é fixo e somente leitura — sem create/update/remove.
  function list()           { return repo.list(); }

  function listCached()     { return cache.costCenters(); }
  function findById(id)     { return cache.findById('costCenters', id); }
  function onChange(cb)     { return cache.subscribe('COST_CENTER', cb); }

  window.App = window.App || {};
  window.App.CostCenterService = {
    init: init,
    list: list, listCached: listCached,
    findById: findById, onChange: onChange,
  };
})();

/* infrastructure/secondary — read-only adapter for the global /cost-center catalog
 * (fixed system data; no user namespace, no mutations). */
(function () {
  function create(http) {
    return {
      list: function () { return http.global.get('/cost-center'); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.CostCenterRepository = { create: create };
})();

/* infrastructure/primary — page Centros de Custo. */
(function () {
  window.Pages = window.Pages || {};

  let state = null;

  function resetState() {
    state = {
      costCenters: [],
      $root: null,
    };
    return state;
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

    const $header = window.pageHeader({ title: 'Centros de Custo' });

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
    const subtitle = window.Domain.CostCenter.displaySubtitle(cc);
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
  // CostCenterService.onChange já existia mas nunca tinha sido conectado aqui — a tela não
  // reagia a mudanças de outras origens (ex. quick-create em import-rules). cachePage conecta.
  window.Pages['cost-centers'] = window.cachePage({
    ns: '.cost-centers',
    collection: 'costCenters',
    event: 'COST_CENTER',
    state: resetState,
    render: render,
  });
})();

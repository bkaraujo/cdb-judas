/* _3_infrastructure/primary/router.js — hash-based router. mounts window.Pages[id] into #page. */

(function () {
  const SCREENS = {
    'dashboard':        'Visão Geral',
    'transactions':     'Lançamentos',
    'accounts-payable': 'A Pagar e Receber',
    'statement':        'Extrato de Contas',
    'credit-cards':     'Cartões de Crédito',
    'budget':           'Metas / Orçamento',
    'reports':          'Relatórios',
    'categories':       'Categorias',
    'cost-centers':     'Centros de Custo',
    'accounts':         'Contas Bancárias',
    'tags':             'Tags',
    'settings':         'Configurações',
  };

  const KEY_SCREEN = 'cbd-screen';
  const state = { current: null };

  function parseHash() {
    const h = (location.hash || '').replace(/^#\/?/, '');
    return SCREENS[h] ? h : null;
  }

  function apply(id) {
    if (!SCREENS[id]) id = 'dashboard';

    // Unmount previous
    if (state.current && window.Pages[state.current] && window.Pages[state.current].unmount) {
      try { window.Pages[state.current].unmount(); } catch (e) { /* swallow */ }
    }

    state.current = id;
    localStorage.setItem(KEY_SCREEN, id);
    document.body.setAttribute('data-screen', id);


    if (window.Sidebar && window.Sidebar.setCurrent) window.Sidebar.setCurrent(id);

    const $page = $('#page').empty();
    const page = window.Pages && window.Pages[id];
    if (page && page.mount) page.mount($page);
    else $page.html(window.emptyState({ icon: 'alertCircle', title: 'Tela não encontrada', desc: id }));
  }

  function onHashChange() {
    const id = parseHash() || localStorage.getItem(KEY_SCREEN) || 'dashboard';
    if (id === state.current) return;
    apply(id);
  }

  window.Router = {
    SCREENS: SCREENS,
    start: function () {
      $(window).on('hashchange', onHashChange);
      const id = parseHash() || localStorage.getItem(KEY_SCREEN) || 'dashboard';
      if (!location.hash) location.hash = '#/' + id;
      else apply(id);
    },
    go: function (id) {
      if (location.hash === '#/' + id) apply(id);
      else location.hash = '#/' + id;
    },
    current: function () { return state.current; },
  };
})();

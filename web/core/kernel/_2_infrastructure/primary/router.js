/* _3_infrastructure/primary/router.js — hash-based router. mounts window.Pages[id] into #page.
 * A rota é `#/<id>` ou `#/<id>/<param>` — só `card-statement` usa o parâmetro hoje (o id do
 * cartão), repassado como 2º argumento de mount(). O que se persiste em `cbd-screen` é a rota
 * INTEIRA, com o parâmetro: guardar só o id faria o boot em `#/card-statement` cair numa tela
 * sem cartão. */

(function () {
  const SCREENS = {
    'dashboard':        'Visão Geral',
    'transactions':     'Lançamentos',
    'installments':     'Parcelamentos',
    'accounts-payable': 'A Pagar e Receber',
    'statement':        'Extrato de Contas',
    'credit-cards':     'Cartões de Crédito',
    // Fora do sidebar (sidebar.data.js): só se chega por link da linha de fatura ou do botão
    // "Ver fatura" — precisa do id do cartão no path.
    'card-statement':   'Extrato do Cartão',
    'budget':           'Metas / Orçamento',
    'reports':          'Relatórios',
    'categories':       'Categorias',
    'cost-centers':     'Centros de Custo',
    'accounts':         'Contas Bancárias',
    'tags':             'Tags',
    'import-rules':     'Regras de Nomenclatura',
    'settings':         'Configurações',
  };

  const KEY_SCREEN = 'cbd-screen';
  const state = { current: null, route: null };

  /* 'card-statement/abc' → { id: 'card-statement', param: 'abc' }; rota desconhecida → null. */
  function parseRoute(route) {
    const clean = String(route || '').replace(/^#\/?/, '');
    if (!clean) return null;
    const slash = clean.indexOf('/');
    const id = slash < 0 ? clean : clean.slice(0, slash);
    if (!SCREENS[id]) return null;
    return { id: id, param: slash < 0 ? null : clean.slice(slash + 1) };
  }

  function routeOf(parsed) {
    return parsed.param ? parsed.id + '/' + parsed.param : parsed.id;
  }

  function apply(parsed) {
    const id = parsed.id;
    const route = routeOf(parsed);

    // Unmount previous
    if (state.current && window.Pages[state.current] && window.Pages[state.current].unmount) {
      try { window.Pages[state.current].unmount(); } catch (e) { /* swallow */ }
    }

    state.current = id;
    state.route = route;
    localStorage.setItem(KEY_SCREEN, route);
    document.body.setAttribute('data-screen', id);


    if (window.Sidebar && window.Sidebar.setCurrent) window.Sidebar.setCurrent(id);

    const $page = $('#page').empty();
    const page = window.Pages && window.Pages[id];
    if (page && page.mount) page.mount($page, parsed.param);
    else $page.html(window.emptyState({ icon: 'alertCircle', title: 'Tela não encontrada', desc: id }));
  }

  function resolve() {
    return parseRoute(location.hash)
        || parseRoute(localStorage.getItem(KEY_SCREEN))
        || { id: 'dashboard', param: null };
  }

  function onHashChange() {
    const parsed = resolve();
    if (routeOf(parsed) === state.route) return;
    apply(parsed);
  }

  window.Router = {
    SCREENS: SCREENS,
    start: function () {
      $(window).on('hashchange', onHashChange);
      const parsed = resolve();
      if (!location.hash) location.hash = '#/' + routeOf(parsed);
      else apply(parsed);
    },
    go: function (route) {
      const parsed = parseRoute(route) || { id: 'dashboard', param: null };
      const hash = '#/' + routeOf(parsed);
      if (location.hash === hash) apply(parsed);
      else location.hash = hash;
    },
    current: function () { return state.current; },
  };
})();

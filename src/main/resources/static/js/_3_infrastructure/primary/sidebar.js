/* _3_infrastructure/primary/sidebar.js — collapsible sidebar with grouped nav + tooltips + theme toggle. */

(function () {
  const NAV = [
    { id: 'dashboard', label: 'Visão Geral', icon: 'home' },
    {
      id: 'movimentacoes', label: 'Movimentações', icon: 'layers',
      children: [
        { id: 'transactions',     label: 'Lançamentos',       icon: 'list' },
        { id: 'accounts-payable', label: 'A pagar e receber', icon: 'calendar' },
      ],
    },
    { id: 'statement',    label: 'Extrato de Contas',  icon: 'bookOpen' },
    { id: 'credit-cards', label: 'Cartões de Crédito', icon: 'creditCard' },
    { id: 'budget',       label: 'Metas / Orçamento',  icon: 'target' },
    { id: 'reports',      label: 'Relatórios',         icon: 'barChart' },
    {
      id: 'cadastros', label: 'Cadastros', icon: 'database',
      children: [
        { id: 'categories',   label: 'Categorias',      icon: 'tag' },
        { id: 'cost-centers', label: 'Centros de Custo', icon: 'briefcase' },
        { id: 'accounts',     label: 'Contas',           icon: 'building' },
        { id: 'tags',         label: 'Tags',             icon: 'hash' },
      ],
    },
  ];

  const KEY_COLLAPSED = 'cbd-sidebar-collapsed';
  const KEY_GROUPS    = 'cbd-sidebar-groups';

  const state = {
    current: 'dashboard',
    collapsed: localStorage.getItem(KEY_COLLAPSED) === '1',
    groups: (function () {
      try { return JSON.parse(localStorage.getItem(KEY_GROUPS)) || { movimentacoes: true, cadastros: false }; }
      catch (e) { return { movimentacoes: true, cadastros: false }; }
    })(),
    onNav: null,
    $root: null,
  };

  function persist() {
    localStorage.setItem(KEY_COLLAPSED, state.collapsed ? '1' : '0');
    localStorage.setItem(KEY_GROUPS, JSON.stringify(state.groups));
  }

  function isActive(item) {
    if (item.id === state.current) return true;
    if (item.children) return item.children.some(function (c) { return c.id === state.current; });
    return false;
  }

  function render() {
    const $s = state.$root;
    $s.toggleClass('collapsed', state.collapsed);
    $s.empty();

    // Head
    const $head = $('<div class="sidebar-head"></div>');
    $head.append(
      '<div class="sidebar-brand">' +
        '<div class="sidebar-logo">C</div>' +
        '<span class="sidebar-brand-name">CBD Finance</span>' +
      '</div>'
    );
    const $collapseBtn = $('<button class="icon-btn sidebar-collapse-btn">' + window.icon('chevronLeft', 16) + '</button>');
    $collapseBtn.on('click', function () { state.collapsed = true; persist(); render(); });
    $head.append($collapseBtn);
    $s.append($head);

    // Nav
    const $nav = $('<nav class="sidebar-nav"></nav>');
    NAV.forEach(function (item) {
      const active   = isActive(item);
      const hasChild = !!item.children;
      const open     = !!state.groups[item.id];

      let classes = 'sidebar-item';
      if (active && !hasChild) classes += ' active';
      else if (active && hasChild) classes += ' group-active';

      const chevron = hasChild
        ? '<span class="sidebar-item-chevron">' + window.icon(open ? 'chevronUp' : 'chevronDown', 13) + '</span>'
        : '';

      const $wrap = $('<div class="sidebar-item-wrap tooltip-wrap"></div>');
      const $btn = $(
        '<button class="' + classes + '" data-id="' + esc(item.id) + '">' +
          window.icon(item.icon, 17) +
          '<span class="sidebar-item-label">' + esc(item.label) + '</span>' +
          chevron +
        '</button>'
      );
      $btn.on('click', function () {
        if (hasChild) {
          if (state.collapsed) { state.collapsed = false; state.groups[item.id] = true; }
          else state.groups[item.id] = !state.groups[item.id];
          persist(); render();
        } else {
          if (state.onNav) state.onNav(item.id);
          if (state.collapsed) { state.collapsed = false; persist(); render(); }
        }
      });
      $wrap.append($btn);
      $wrap.append('<span class="tooltip">' + esc(item.label) + '</span>');
      $nav.append($wrap);

      if (hasChild && open && !state.collapsed) {
        const $kids = $('<div class="sidebar-children"></div>');
        item.children.forEach(function (child) {
          const childActive = child.id === state.current;
          const $kw = $('<div class="tooltip-wrap"></div>');
          const $kb = $(
            '<button class="sidebar-child' + (childActive ? ' active' : '') + '" data-id="' + esc(child.id) + '">' +
              window.icon(child.icon, 14) +
              '<span>' + esc(child.label) + '</span>' +
            '</button>'
          );
          $kb.on('click', function () { if (state.onNav) state.onNav(child.id); });
          $kw.append($kb);
          $kids.append($kw);
        });
        $nav.append($kids);
      }
    });
    $s.append($nav);

    // Foot
    const $foot = $('<div class="sidebar-foot"></div>');
    const themeName = window.Theme.get() === 'dark' ? 'sun' : 'moon';
    const themeLbl  = window.Theme.get() === 'dark' ? 'Modo Claro' : 'Modo Escuro';
    const $themeWrap = $('<div class="tooltip-wrap"></div>');
    const $themeBtn = $('<button class="icon-btn">' + window.icon(themeName, 16) +
      '<span class="sidebar-foot-label">' + esc(themeLbl) + '</span></button>');
    $themeBtn.on('click', function () { window.Theme.toggle(); render(); });
    $themeWrap.append($themeBtn);
    $themeWrap.append('<span class="tooltip">' + esc(themeLbl) + '</span>');
    $foot.append($themeWrap);

    if (state.collapsed) {
      const $expWrap = $('<div class="tooltip-wrap"></div>');
      const $expBtn = $('<button class="icon-btn">' + window.icon('chevronRight', 16) + '</button>');
      $expBtn.on('click', function () { state.collapsed = false; persist(); render(); });
      $expWrap.append($expBtn);
      $expWrap.append('<span class="tooltip">Expandir menu</span>');
      $foot.append($expWrap);
    }
    $s.append($foot);
  }

  window.Sidebar = {
    mount: function ($root, opts) {
      opts = opts || {};
      state.$root = $root.addClass('sidebar');
      state.current = opts.current || state.current;
      state.onNav = opts.onNav || null;
      render();
    },
    setCurrent: function (id) {
      state.current = id;
      // Auto-open parent group if current is a child.
      NAV.forEach(function (item) {
        if (item.children && item.children.some(function (c) { return c.id === id; })) {
          state.groups[item.id] = true;
        }
      });
      persist();
      if (state.$root) render();
    },
  };
})();

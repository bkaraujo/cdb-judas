/* _3_infrastructure/primary/sidebar.js — collapsible sidebar with grouped nav + tooltips + theme toggle. */

(function () {
  // NAV vem de window.SIDEBAR_NAV (sidebar.data.js, carregado via <script> no barrel).
  // Promise resolvida mantém a API dos callers; funciona sob file:// (sem fetch/CORS).
  let NAV = [];
  let navPromise = null;

  function loadNav() {
    if (navPromise) return navPromise;
    NAV = window.SIDEBAR_NAV || [];
    if (!NAV.length) console.error('Sidebar: window.SIDEBAR_NAV ausente (sidebar.data.js não carregou)');
    navPromise = Promise.resolve(NAV);
    return navPromise;
  }

  const DEFAULT_GROUPS = { movements: true, registries: false };

  const state = {
    current: 'dashboard',
    collapsed: false,
    groups: Object.assign({}, DEFAULT_GROUPS),
    version: '0.0.0',
    closing: null,
    onNav: null,
    $root: null,
  };

  // Collapse + group state live in PreferencesService: collapse is server-synced
  // (write-through), groups are local-only. The service is wired after this module loads,
  // so state is pulled at mount/reconcile time — not at IIFE load.
  function prefs() { return window.App && window.App.PreferencesService; }

  function loadState() {
    const p = prefs();
    if (!p) return;
    state.collapsed = p.getSidebarCollapsed();
    const groups = p.getSidebarGroups();
    state.groups = (groups && Object.keys(groups).length) ? groups : Object.assign({}, DEFAULT_GROUPS);
  }

  function persist() {
    const p = prefs();
    if (!p) return;
    p.setSidebarCollapsed(state.collapsed); // write-through: mirror + debounced PATCH /api/me
    p.setSidebarGroups(state.groups);       // local-only
  }

  // Initial letter of the display name (name ?? username); falls back to 'C' when unknown.
  function avatarInitial() {
    const store = window.Infra && window.Infra.AuthStore;
    let who = '';
    if (store) {
      who = store.name() || '';
      if (!who) {
        const u = store.decodeUser();
        if (u && u !== 'anonymous') who = u;
      }
    }
    who = (who || '').trim();
    return who ? who.charAt(0).toUpperCase() : 'C';
  }

  // "2026-06" -> "06/2026" for the closing tooltip.
  function closingLabel(period) {
    const parts = period.split('-');
    return parts[1] + '/' + parts[0];
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
    const $brand = $('<div class="sidebar-brand"></div>');
    $brand.append(
      '<div class="sidebar-logo">C</div>' +
      '<div class="sidebar-brand-text">' +
        '<span class="sidebar-brand-name">CBD Finance</span>' +
        '<span class="sidebar-version">v' + state.version + '</span>' +
      '</div>'
    );
    $head.append($brand);
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

    // User avatar (left) — initial of the display name (name ?? username); opens Configurações.
    const $avatarWrap = $('<div class="tooltip-wrap"></div>');
    const $avatar = $('<div class="sidebar-avatar" style="cursor:pointer;"></div>').text(avatarInitial());
    $avatar.on('click', function () { if (window.Router && window.Router.go) window.Router.go('settings'); });
    $avatarWrap.append($avatar);
    $avatarWrap.append('<span class="tooltip">Perfil</span>');
    $foot.append($avatarWrap);

    // Accounting closing lock (right, before theme) — icon reflects whether a period is set.
    const closingLbl = state.closing
      ? 'Fechamento: ' + esc(closingLabel(state.closing))
      : 'Fechamento contábil';
    const $closingWrap = $('<div class="tooltip-wrap sidebar-foot-right"></div>');
    const $closingBtn = $(
      '<button class="icon-btn"' + (state.closing ? ' style="color:var(--accent);"' : '') + '>' +
        window.icon(state.closing ? 'lock' : 'unlock', 16) +
      '</button>'
    );
    $closingBtn.on('click', function () {
      window.closingDialog({
        current: state.closing,
        onChange: function (p) { state.closing = p; render(); },
      });
    });
    $closingWrap.append($closingBtn);
    $closingWrap.append('<span class="tooltip">' + closingLbl + '</span>');
    $foot.append($closingWrap);

    // Theme toggle icon (right)
    const $themeWrap = $('<div class="tooltip-wrap"></div>');
    const $themeBtn = $('<button class="icon-btn">' + window.icon(themeName, 16) + '</button>');
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
      loadState();
      loadNav().then(render);
      this.refreshVersion();
    },
    refreshVersion: function () {
      if (window.App && window.App.SystemService) {
        window.App.SystemService.getVersion()
          .then(function (res) {
            state.version = res.version || '0.0.0';
            if (state.$root) render();
          })
          .catch(function (err) {
            console.warn('Sidebar: falha ao obter versão', err);
          });
      }
    },
    setCurrent: function (id) {
      state.current = id;
      loadNav().then(function () {
        // Auto-open parent group if current is a child.
        NAV.forEach(function (item) {
          if (item.children && item.children.some(function (c) { return c.id === id; })) {
            state.groups[item.id] = true;
          }
        });
        persist();
        if (state.$root) render();
      });
    },
    // Re-render to reflect a changed display name on the avatar (after a profile save).
    refreshUser: function () {
      if (state.$root) render();
    },
    // Re-read collapse/group state from preferences (after login reconciliation) and re-render.
    refreshState: function () {
      loadState();
      if (state.$root) render();
    },
    // Pulls the active accounting-closing period. Not called from mount() — an anonymous
    // GET before login would 401; composition-root calls this from hydrateSelf() instead.
    refreshClosing: function () {
      if (window.App && window.App.ClosingService) {
        window.App.ClosingService.get()
          .then(function (res) {
            state.closing = res && res.period;
            if (state.$root) render();
          })
          .catch(function (err) {
            console.warn('Sidebar: falha ao obter fechamento', err);
          });
      }
    },
  };
})();

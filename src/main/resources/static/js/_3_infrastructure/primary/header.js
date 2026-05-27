/* _3_infrastructure/primary/header.js — top bar with search, notifications, theme, avatar. */

(function () {
  const state = { $root: null, onMenuToggle: null, title: '' };

  function render() {
    const $h = state.$root;
    const themeIcon = window.Theme.get() === 'dark' ? 'sun' : 'moon';
    const themeTitle = window.Theme.get() === 'dark' ? 'Modo claro' : 'Modo escuro';

    $h.empty().append(
      '<div class="header-left">' +
        '<button class="icon-btn" data-act="menu">' + window.icon('menu', 18) + '</button>' +
        '<span class="header-title">' + esc(state.title) + '</span>' +
      '</div>' +
      '<div class="header-right">' +
        '<div class="header-search">' +
          '<span class="search-icon">' + window.icon('search', 15) + '</span>' +
          '<input type="text" placeholder="Buscar...">' +
        '</div>' +
        '<button class="icon-btn" style="position:relative" data-act="bell">' +
          window.icon('bell', 17) +
          '<span class="header-bell-dot"></span>' +
        '</button>' +
        '<button class="icon-btn" data-act="theme" title="' + esc(themeTitle) + '">' +
          window.icon(themeIcon, 17) +
        '</button>' +
        '<div class="header-avatar">C</div>' +
      '</div>'
    );

    $h.find('[data-act=menu]').on('click', function () {
      if (state.onMenuToggle) state.onMenuToggle();
    });
    $h.find('[data-act=theme]').on('click', function () {
      window.Theme.toggle();
      render();
    });
  }

  window.Header = {
    mount: function ($root, opts) {
      opts = opts || {};
      state.$root = $root.addClass('header');
      state.onMenuToggle = opts.onMenuToggle || null;
      render();
    },
    setTitle: function (t) {
      state.title = t || '';
      if (state.$root) state.$root.find('.header-title').text(state.title);
    },
  };
})();

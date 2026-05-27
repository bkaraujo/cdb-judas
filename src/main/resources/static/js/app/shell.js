/* app/shell.js — top-level DOM scaffold + sidebar/header mount + nav. */
(function () {
  function buildShell() {
    const $app = $('#app');
    $app.html(
      '<div class="app-layout">' +
        '<aside id="sidebar"></aside>' +
        '<div class="main-area">' +
          '<header id="header"></header>' +
          '<main class="page-content" id="page"></main>' +
        '</div>' +
      '</div>'
    );
  }

  function navTo(id) { window.Router.go(id); }

  function toggleSidebarFromHeader() {
    const $s = $('#sidebar');
    const collapsed = !$s.hasClass('collapsed');
    window.App.PreferencesService.setSidebarCollapsed(collapsed);
    window.Sidebar.mount($s, { current: window.Router.current(), onNav: navTo });
  }

  function mountChrome(initialScreenId) {
    window.Sidebar.mount($('#sidebar'), { current: initialScreenId, onNav: navTo });
    window.Header.mount($('#header'),  { onMenuToggle: toggleSidebarFromHeader });
  }

  window.App = window.App || {};
  window.App.Shell = {
    buildShell: buildShell,
    mountChrome: mountChrome,
    navTo: navTo,
  };
})();

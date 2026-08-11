/* app/shell.js — top-level DOM scaffold + sidebar mount + nav. */
(function () {
  function buildShell() {
    const $app = $('#app');
    $app.html(
      '<div class="app-layout">' +
        '<aside id="sidebar"></aside>' +
        '<div class="main-area">' +
          '<main class="page-content" id="page"></main>' +
        '</div>' +
      '</div>'
    );
  }

  function navTo(id) { window.Router.go(id); }

  function mountChrome(initialScreenId) {
    window.Sidebar.mount($('#sidebar'), { current: initialScreenId, onNav: navTo });
  }

  window.App = window.App || {};
  window.App.Shell = {
    buildShell: buildShell,
    mountChrome: mountChrome,
    navTo: navTo,
  };
})();

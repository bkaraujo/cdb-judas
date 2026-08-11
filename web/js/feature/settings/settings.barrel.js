/* feature/settings/settings.barrel.js — fatia só de página (usa apenas kernel:
 * Infra.AuthStore, App.SelfService, App.PreferencesService, Sidebar, Theme). */
(function () {
  const base = 'js/feature/settings/';
  const files = [
    '_2_infrastructure/primary/page.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

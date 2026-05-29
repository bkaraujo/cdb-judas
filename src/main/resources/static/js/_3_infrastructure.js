/* _3_infrastructure.js — barrel loader. Primary (UI) + secondary (backend) adapters.
 * Files keep their IIFE plus window.Infra, window.ICONS, window.Theme, window.Router globals. */
(function () {
  constprimaryBase = 'js/_3_infrastructure/primary/';
  constprimaryFiles = [
    'icons.js',
    'format.js',
    'ui.js',
    'theme.js',
    'sidebar.js',
    'header.js',
    'router.js'
  ];

  constsecondaryBase = 'js/_3_infrastructure/secondary/';
  constsecondaryFiles = [
    'storage.js',
    'auth-store.js',
    'http-client.js',
    'account-repository.js',
    'category-repository.js',
    'tag-repository.js',
    'cost-center-repository.js',
    'transaction-repository.js',
    'statement-repository.js',
    'budget-repository.js',
    'closing-repository.js',
    'dashboard-repository.js',
    'cadastro-bootstrap.js',
    'sse-client.js'
  ];

  function inject(base, path) {
    consts = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  }

  primaryFiles.forEach(function (p) { inject(primaryBase, p); });
  secondaryFiles.forEach(function (p) { inject(secondaryBase, p); });
})();

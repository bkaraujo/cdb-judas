/* _3_infrastructure.js — barrel loader. Primary (UI) + secondary (backend) adapters.
 * Files keep their IIFE plus window.Infra, window.ICONS, window.Theme, window.Router globals. */
(function () {
  const primaryBase = 'js/_3_infrastructure/primary/';
  const primaryFiles = [
    'icons.js',
    'format.js',
    'ui.js',
    'helpers.js',
    'theme.js',
    'sidebar.data.js',
    'sidebar.js',

    'router.js'
  ];

  const secondaryBase = 'js/_3_infrastructure/secondary/';
  const secondaryFiles = [
    'storage.js',
    'auth-store.js',
    'http-client.js',
    'self-repository.js',
    'account-repository.js',
    'category-repository.js',
    'tag-repository.js',
    'cost-center-repository.js',
    'transaction-repository.js',
    'statement-repository.js',
    'budget-repository.js',
    'closing-repository.js',
    'dashboard-repository.js',
    'registry-bootstrap.js',
    'sse-client.js'
  ];

  function inject(base, path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  }

  primaryFiles.forEach(function (p) { inject(primaryBase, p); });
  secondaryFiles.forEach(function (p) { inject(secondaryBase, p); });
})();

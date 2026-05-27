/* _3_infrastructure.js — barrel loader. Primary (UI) + secondary (backend) adapters.
 * Files keep their IIFE plus window.Infra, window.ICONS, window.Theme, window.Router globals. */
(function () {
  var primaryBase = 'js/_3_infrastructure/primary/';
  var primaryFiles = [
    'icons.js',
    'format.js',
    'ui.js',
    'theme.js',
    'sidebar.js',
    'header.js',
    'router.js'
  ];

  var secondaryBase = 'js/_3_infrastructure/secondary/';
  var secondaryFiles = [
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
    'payable-repository.js',
    'closing-repository.js',
    'dashboard-repository.js',
    'credit-card-repository.js',
    'cadastro-bootstrap.js',
    'sse-client.js'
  ];

  function inject(base, path) {
    var s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  }

  primaryFiles.forEach(function (p) { inject(primaryBase, p); });
  secondaryFiles.forEach(function (p) { inject(secondaryBase, p); });
})();

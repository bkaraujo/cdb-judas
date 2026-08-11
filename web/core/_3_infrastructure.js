/* _3_infrastructure.js — barrel loader. Primary (UI) + secondary (backend) adapters.
 * Files keep their IIFE plus window.Infra, window.ICONS, window.Theme, window.Router globals. */
(function () {
  const primaryBase = 'core/_3_infrastructure/primary/';
  const primaryFiles = [];

  const secondaryBase = 'core/_3_infrastructure/secondary/';
  const secondaryFiles = [
    'budget-repository.js',
    'dashboard-repository.js'
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

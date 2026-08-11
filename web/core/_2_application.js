/* _2_application.js — barrel loader. Use cases / ports.
 * Files keep their IIFE + window.App.* globals. */
(function () {
  const base = 'core/_2_application/';
  const files = [
    'transaction-service.js',
    'payable-service.js',
    'statement-service.js',
    'budget-service.js',
    'credit-card-service.js',
    'dashboard-service.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

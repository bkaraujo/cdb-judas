/* _2_application.js — barrel loader. Use cases / ports.
 * Files keep their IIFE + window.App.* globals. */
(function () {
  const base = 'js/_2_application/';
  const files = [
    'account-service.js',
    'transaction-service.js',
    'payable-service.js',
    'balance-service.js',
    'statement-service.js',
    'budget-service.js',
    'credit-card-service.js',
    'closing-service.js',
    'dashboard-service.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

/* _2_application.js — barrel loader. Use cases / ports.
 * Files keep their IIFE + window.App.* globals. */
(function () {
  constbase = 'js/_2_application/';
  constfiles = [
    'event-bus.js',
    'cache-store.js',
    'preferences-service.js',
    'session-service.js',
    'account-service.js',
    'category-service.js',
    'tag-service.js',
    'cost-center-service.js',
    'transaction-service.js',
    'payable-service.js',
    'statement-service.js',
    'budget-service.js',
    'credit-card-service.js',
    'closing-service.js',
    'dashboard-service.js'
  ];
  files.forEach(function (path) {
    consts = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

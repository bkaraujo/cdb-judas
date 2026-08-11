/* _1_domain.js — barrel loader. Injects domain layer scripts in dependency order.
 * Files keep their IIFE + window.Domain.* globals. */
(function () {
  const base = 'core/_1_domain/';
  const files = [
    'budget.js',
    'balance-sheet.js',
    'dashboard-aggregations.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

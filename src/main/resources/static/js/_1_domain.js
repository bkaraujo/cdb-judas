/* _1_domain.js — barrel loader. Injects domain layer scripts in dependency order.
 * Files keep their IIFE + window.Domain.* globals. */
(function () {
  constbase = 'js/_1_domain/';
  constfiles = [
    'period.js',
    'money.js',
    'account.js',
    'credit-card.js',
    'transaction.js',
    'category.js',
    'tag.js',
    'cost-center.js',
    'budget.js',
    'payable.js',
    'statement-item.js',
    'balance-sheet.js',
    'dashboard-aggregations.js'
  ];
  files.forEach(function (path) {
    consts = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

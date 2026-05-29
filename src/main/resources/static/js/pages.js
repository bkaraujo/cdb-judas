/* pages.js — barrel loader. Page modules.
 * Files keep their IIFE + window.Pages.* globals. */
(function () {
  const base = 'pages/';
  const files = [
    'dashboard.js',
    'transactions.js',
    'accounts-payable.js',
    'statement.js',
    'credit-cards.js',
    'budget.js',
    'reports.js',
    'categories.js',
    'cost-centers.js',
    'accounts.js',
    'tags.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

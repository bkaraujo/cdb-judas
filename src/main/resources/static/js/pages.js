/* pages.js — barrel loader. Page modules.
 * Files keep their IIFE + window.Pages.* globals. */
(function () {
  var base = 'pages/';
  var files = [
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
    var s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

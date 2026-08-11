/* pages.js — barrel loader. Page modules.
 * Files keep their IIFE + window.Pages.* globals. */
(function () {
  const base = 'pages/';
  const files = [
    'dashboard/cash-balances.js',
    'dashboard/month-result.js',
    'dashboard/expenses-by-category.js',
    'dashboard/revenues-by-category.js',
    'dashboard/accounts-payable.js',
    'dashboard/credit-cards.js',
    'dashboard/cash-flow.js',
    'dashboard/expense-goals.js',
    'dashboard/recent-postings.js',
    'dashboard/balance-sheet.js',
    'dashboard.js',
    'transactions/import-statement.js',
    'accounts-payable.js',
    'credit-cards.js',
    'card-statement.js',
    'budget.js'
  ];
  files.forEach(function (path) {
    const s = document.createElement('script');
    s.src = base + path;
    s.async = false;
    document.head.appendChild(s);
  });
})();

/* _2_application/dashboard-service.js — dashboard data orchestration. */
(function () {
  let repo = null;
  let txRepo = null;
  let payableRepo = null;

  function init(deps) {
    repo        = deps.repo;
    txRepo      = deps.txRepo;
    payableRepo = deps.payableRepo;
    return { ready: true };
  }

  function monthlyResult(period) {
    return repo.getMonthlyResult(period.month, period.year);
  }

  function recentTransactions(limit) {
    return repo.getRecentTransactions(limit);
  }

  /* Loads everything dashboard panels need in parallel. */
  function loadAll(period) {
    const b = window.Domain.Period.bounds(period);
    return Promise.all([
      txRepo.list('from=' + b.from + '&to=' + b.to),
      payableRepo.listPayable(),
      payableRepo.listReceivable(),
    ]).then(function (arr) {
      return {
        transactions: Array.isArray(arr[0]) ? arr[0] : [],
        payables:     Array.isArray(arr[1]) ? arr[1] : [],
        receivables:  Array.isArray(arr[2]) ? arr[2] : [],
      };
    });
  }

  window.App = window.App || {};
  window.App.DashboardService = {
    init: init,
    monthlyResult: monthlyResult,
    recentTransactions: recentTransactions,
    loadAll: loadAll,
  };
})();

/* _2_application/dashboard-service.js — dashboard data orchestration. */
(function () {
  let repo = null;
  let txRepo = null;

  function init(deps) {
    repo        = deps.repo;
    txRepo      = deps.txRepo;
    return { ready: true };
  }

  // A Pagar/Receber derivam de transações pendentes (sem recurso de payables).
  function adaptPending(label) {
    return function (txs) {
      return (Array.isArray(txs) ? txs : []).map(function (t) {
        return {
          id: t.id, description: t.description, due: t.date,
          amount: Math.abs(+t.amount || 0), accountId: t.accountId,
          categoryId: t.categoryId, status: t.status, type: label,
        };
      });
    };
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
      txRepo.list('status=pending&type=expense').then(adaptPending('PAYABLE')),
      txRepo.list('status=pending&type=income').then(adaptPending('RECEIVABLE')),
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

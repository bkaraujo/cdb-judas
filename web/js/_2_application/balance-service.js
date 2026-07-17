/* _2_application/balance-service.js — monthly-balance reads.
 * Single owner of monthly-balance snapshot access for the frontend. */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  /* Resolves to the MonthlyBalance { id, accountId, period, balance } for the
     account/period, or rejects with err.status === 404 when no snapshot exists. */
  function monthly(accountId, period) {
    return repo.monthly(accountId, window.Domain.Period.yyyymm(period));
  }

  /* Resolves to BalanceResponse[] ({ accountId, period, balance }) for every account of the
     current user in the given period, in a single request — accounts without a snapshot for
     the period (e.g. before their first activity) are simply absent from the list. */
  function allAccounts(period) {
    return repo.allAccounts(window.Domain.Period.yyyymm(period));
  }

  window.App = window.App || {};
  window.App.BalanceService = {
    init: init,
    monthly: monthly,
    allAccounts: allAccounts,
  };
})();

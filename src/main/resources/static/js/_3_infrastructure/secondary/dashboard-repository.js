/* _3_infrastructure/secondary/dashboard-repository.js — HTTP adapter for /dashboard endpoints. */
(function () {
  function create(http) {
    return {
      getMonthlyResult: function (month, year) {
        return http.get('/dashboard/result?month=' + month + '&year=' + year);
      },
      getRecentTransactions: function (limit) {
        return http.get('/transactions?limit=' + (limit || 5) + '&sort=date,desc');
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.DashboardRepository = { create: create };
})();

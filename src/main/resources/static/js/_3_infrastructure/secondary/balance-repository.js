/* _3_infrastructure/secondary/balance-repository.js — HTTP adapter for the monthly
 * account-balance snapshot under the accounts namespace: GET /accounts/{id}/balance?period=yyyyMM. */
(function () {
  function create(http) {
    return {
      monthly: function (id, yyyyMM) {
        return http.get('/accounts/' + id + '/balance?period=' + yyyyMM);
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.BalanceRepository = { create: create };
})();

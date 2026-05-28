/* _3_infrastructure/secondary/statement-repository.js — HTTP adapter for statements
 * under the accounts namespace: detail per account + month summary per account. */
(function () {
  function create(http) {
    return {
      detail: function (accountId, yyyyMM, status) {
        return http.get('/accounts/' + accountId + '/statements/' + yyyyMM + (status ? '?status=' + status : ''));
      },
      summary: function (yyyyMM, status) {
        return http.get('/accounts/statements/' + yyyyMM + (status ? '?status=' + status : ''));
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.StatementRepository = { create: create };
})();

/* _3_infrastructure/secondary/statement-repository.js — HTTP adapter for /statement. */
(function () {
  function create(http) {
    return {
      list: function (accountId, month, year) {
        return http.get('/statement?accountId=' + accountId + '&month=' + month + '&year=' + year);
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.StatementRepository = { create: create };
})();

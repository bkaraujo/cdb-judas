/* _3_infrastructure/secondary/closing-repository.js — HTTP adapter for /accounts/closing. */
(function () {
  function create(http) {
    return {
      get:   function ()       { return http.user.get('/accounts/closing'); },
      set:   function (period) { return http.user.post('/accounts/closing', { period: period }); },
      clear: function ()       { return http.user.delete('/accounts/closing'); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.ClosingRepository = { create: create };
})();

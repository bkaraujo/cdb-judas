/* _3_infrastructure/secondary/closing-repository.js — HTTP adapter for /operations/closing. */
(function () {
  function create(http) {
    return {
      get:   function ()       { return http.get('/operations/closing'); },
      set:   function (period) { return http.post('/operations/closing', { period: period }); },
      clear: function ()       { return http.delete('/operations/closing'); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.ClosingRepository = { create: create };
})();

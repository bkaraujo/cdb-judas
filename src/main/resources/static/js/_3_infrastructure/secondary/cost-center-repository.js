/* _3_infrastructure/secondary/cost-center-repository.js — read-only adapter for the global
 * /cost-center catalog (fixed system data; no user namespace, no mutations). */
(function () {
  function create(http) {
    return {
      list: function () { return http.global.get('/cost-center'); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.CostCenterRepository = { create: create };
})();

/* _3_infrastructure/secondary/self-repository.js — HTTP adapter for the self resource (/api/me).
 * Uses the global (no user-prefix) namespace: identity comes from the authenticated context. */
(function () {
  function create(http) {
    return {
      get: function () { return http.global.get('/me'); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.SelfRepository = { create: create };
})();

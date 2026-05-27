/* _3_infrastructure/secondary/credit-card-repository.js — HTTP adapter for /credit-cards. */
(function () {
  function create(http) {
    return {
      list: function () { return http.get('/credit-cards'); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.CreditCardRepository = { create: create };
})();

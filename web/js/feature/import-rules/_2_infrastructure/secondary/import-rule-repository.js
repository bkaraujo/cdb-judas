/* _3_infrastructure/secondary/import-rule-repository.js — HTTP adapter for /accounts/transaction/rules. */
(function () {
  function create(http) {
    return {
      list:   function ()      { return http.get('/accounts/transaction/rules'); },
      create: function (data)  { return http.post('/accounts/transaction/rules', data); },
      update: function (id, d) { return http.patch('/accounts/transaction/rules/' + id, d); },
      remove: function (id)    { return http.delete('/accounts/transaction/rules/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.ImportRuleRepository = { create: create };
})();

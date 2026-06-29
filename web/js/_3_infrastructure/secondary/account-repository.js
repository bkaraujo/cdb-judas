/* _3_infrastructure/secondary/account-repository.js — HTTP adapter for /accounts. */
(function () {
  function create(http) {
    return {
      list:    function ()        { return http.get('/accounts'); },
      create:  function (data)    { return http.post('/accounts', data); },
      update:  function (id, d)   { return http.patch('/accounts/' + id, d); },
      remove:  function (id)      { return http.delete('/accounts/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.AccountRepository = { create: create };
})();

/* _3_infrastructure/secondary/account-repository.js — HTTP adapter for /accounts (+ nested cards). */
(function () {
  function create(http) {
    return {
      list:    function ()        { return http.get('/accounts'); },
      create:  function (data)    { return http.post('/accounts', data); },
      update:  function (id, d)   { return http.patch('/accounts/' + id, d); },
      remove:  function (id)      { return http.delete('/accounts/' + id); },
      listCards:  function (accountId)         { return http.get('/accounts/' + accountId + '/cards'); },
      createCard: function (accountId, data)   { return http.post('/accounts/' + accountId + '/cards', data); },
      removeCard: function (accountId, cardId) { return http.delete('/accounts/' + accountId + '/cards/' + cardId); },
      setCardActive: function (accountId, cardId, active) {
        return http.patch('/accounts/' + accountId + '/cards/' + cardId, { active: active });
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.AccountRepository = { create: create };
})();

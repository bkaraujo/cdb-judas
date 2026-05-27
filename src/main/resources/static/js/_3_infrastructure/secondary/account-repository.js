/* _3_infrastructure/secondary/account-repository.js — HTTP adapter for /accounts. */
(function () {
  function create(http) {
    return {
      list:    function ()        { return http.get('/accounts'); },
      create:  function (data)    { return http.post('/accounts', data); },
      update:  function (id, d)   { return http.patch('/accounts/' + id, d); },
      remove:  function (id)      { return http.delete('/accounts/' + id); },
      getBalance: function (id, params) {
        const q = new URLSearchParams();
        if (params && params.period) q.set('period', params.period);
        if (params && params.year)   q.set('year', String(params.year));
        return http.get('/accounts/' + id + '/balance?' + q.toString());
      },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.AccountRepository = { create: create };
})();

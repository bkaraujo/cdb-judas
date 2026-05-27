/* _3_infrastructure/secondary/budget-repository.js — HTTP adapter for /budget. */
(function () {
  function create(http) {
    return {
      list:   function (month, year) { return http.get('/budget?month=' + month + '&year=' + year); },
      create: function (data)        { return http.post('/budget', data); },
      update: function (id, data)    { return http.patch('/budget/' + id, data); },
      remove: function (id)          { return http.delete('/budget/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.BudgetRepository = { create: create };
})();

/* _3_infrastructure/secondary/cost-center-repository.js — HTTP adapter for /cost-centers. */
(function () {
  function create(http) {
    return {
      list:   function ()        { return http.get('/cost-centers'); },
      create: function (data)    { return http.post('/cost-centers', data); },
      update: function (id, d)   { return http.patch('/cost-centers/' + id, d); },
      remove: function (id)      { return http.delete('/cost-centers/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.CostCenterRepository = { create: create };
})();

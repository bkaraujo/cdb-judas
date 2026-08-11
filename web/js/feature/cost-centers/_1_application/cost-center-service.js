/* _2_application/cost-center-service.js — CostCenter use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  // Centro de custo é fixo e somente leitura — sem create/update/remove.
  function list()           { return repo.list(); }

  function listCached()     { return cache.costCenters(); }
  function findById(id)     { return cache.findById('costCenters', id); }
  function onChange(cb)     { return cache.subscribe('COST_CENTER', cb); }

  window.App = window.App || {};
  window.App.CostCenterService = {
    init: init,
    list: list, listCached: listCached,
    findById: findById, onChange: onChange,
  };
})();

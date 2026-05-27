/* _2_application/cost-center-service.js — CostCenter use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()           { return repo.list(); }
  function create(data)     { return repo.create(data); }
  function update(id, data) { return repo.update(id, data); }
  function remove(id)       { return repo.remove(id); }

  function listCached()     { return cache.costCenters(); }
  function findById(id)     { return cache.findById('costCenters', id); }
  function onChange(cb)     { return cache.subscribe('COST_CENTER', cb); }

  window.App = window.App || {};
  window.App.CostCenterService = {
    init: init,
    list: list, listCached: listCached,
    create: create, update: update, remove: remove,
    findById: findById, onChange: onChange,
  };
})();

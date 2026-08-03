/* _2_application/import-rule-service.js — ImportRule (regra de nomenclatura) use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()           { return repo.list(); }
  function create(data)     { return repo.create(data); }
  function update(id, data) { return repo.update(id, data); }
  function remove(id)       { return repo.remove(id); }

  function listCached()     { return cache.importRules(); }
  function findById(id)     { return cache.findById('importRules', id); }
  function onChange(cb)     { return cache.subscribe('IMPORT_RULE', cb); }

  window.App = window.App || {};
  window.App.ImportRuleService = {
    init: init,
    list: list, listCached: listCached,
    create: create, update: update, remove: remove,
    findById: findById, onChange: onChange,
  };
})();

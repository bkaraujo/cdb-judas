/* _2_application/tag-service.js — Tag use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()           { return repo.list(); }
  function create(data)     { return repo.create(data); }
  function update(id, data) { return repo.update(id, data); }
  function remove(id)       { return repo.remove(id); }

  function listCached()     { return cache.tags(); }
  function findById(id)     { return cache.findById('tags', id); }
  function onChange(cb)     { return cache.subscribe('TAG', cb); }

  window.App = window.App || {};
  window.App.TagService = {
    init: init,
    list: list, listCached: listCached,
    create: create, update: update, remove: remove,
    findById: findById, onChange: onChange,
  };
})();

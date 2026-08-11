/* _2_application/category-service.js — Category use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()           { return repo.list(); }
  function create(data)     { return repo.create(data); }
  function update(id, data) { return repo.update(id, data); }
  function remove(id, opts) { return repo.remove(id, opts); }

  function listCached()     { return cache.categories(); }
  function findById(id)     { return cache.findById('categories', id); }

  function rootsByNature(nature) { return window.Domain.Category.rootsByNature(cache.categories(), nature); }
  function childrenOf(parentId)  { return window.Domain.Category.childrenOf(cache.categories(), parentId); }
  function labelChain(id)        { return window.Domain.Category.labelChain(cache.categories(), id); }
  function eligibleParents(nature, excludeId) {
    return window.Domain.Category.eligibleParents(cache.categories(), nature, excludeId);
  }
  function isEffectivelyActive(id) { return window.Domain.Category.isEffectivelyActive(cache.categories(), id); }

  function onChange(cb) { return cache.subscribe('CATEGORY', cb); }

  window.App = window.App || {};
  window.App.CategoryService = {
    init: init,
    list: list,
    listCached: listCached,
    create: create,
    update: update,
    remove: remove,
    findById: findById,
    rootsByNature: rootsByNature,
    childrenOf: childrenOf,
    labelChain: labelChain,
    eligibleParents: eligibleParents,
    isEffectivelyActive: isEffectivelyActive,
    onChange: onChange,
  };
})();

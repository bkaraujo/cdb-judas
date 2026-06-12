/* _2_application/account-service.js — Account use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list()              { return repo.list(); }
  function create(data)        { return repo.create(data); }
  function update(id, data)    { return repo.update(id, data); }
  function remove(id)          { return repo.remove(id); }

  /* List from cache (no network) — used by views that need accounts in sync
     with SSE updates. */
  function listCached()        { return cache.accounts(); }

  function linkableCheckings(excludeId) {
    return window.Domain.Account.linkableCheckings(cache.accounts(), excludeId);
  }

  function findById(id) { return cache.findById('accounts', id); }

  function onChange(cb) { return cache.subscribe(['ACCOUNT', 'CREDITCARD'], cb); }

  window.App = window.App || {};
  window.App.AccountService = {
    init: init,
    list: list,
    listCached: listCached,
    create: create,
    update: update,
    remove: remove,
    linkableCheckings: linkableCheckings,
    findById: findById,
    onChange: onChange,
  };
})();

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

  function findById(id) { return cache.findById('accounts', id); }

  function addCard(accountId, data)      { return repo.createCard(accountId, data); }
  function removeCard(accountId, cardId) { return repo.removeCard(accountId, cardId); }
  function setCardActive(accountId, cardId, active) { return repo.setCardActive(accountId, cardId, active); }

  function onChange(cb) { return cache.subscribe('ACCOUNT', cb); }

  window.App = window.App || {};
  window.App.AccountService = {
    init: init,
    list: list,
    listCached: listCached,
    create: create,
    update: update,
    remove: remove,
    addCard: addCard,
    removeCard: removeCard,
    setCardActive: setCardActive,
    findById: findById,
    onChange: onChange,
  };
})();

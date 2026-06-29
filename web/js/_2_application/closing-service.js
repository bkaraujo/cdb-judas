/* _2_application/closing-service.js — accounting closing period. */
(function () {
  let repo = null;
  function init(deps) { repo = deps.repo; return { ready: true }; }

  function get()         { return repo.get(); }
  function set(period)   { return repo.set(period); }
  function clear()       { return repo.clear(); }

  window.App = window.App || {};
  window.App.ClosingService = {
    init: init, get: get, set: set, clear: clear,
  };
})();

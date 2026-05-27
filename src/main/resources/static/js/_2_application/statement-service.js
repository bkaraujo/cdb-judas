/* _2_application/statement-service.js — Statement use cases. */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  function load(accountId, period) {
    return repo.list(accountId, period.month, period.year);
  }

  window.App = window.App || {};
  window.App.StatementService = {
    init: init,
    load: load,
  };
})();

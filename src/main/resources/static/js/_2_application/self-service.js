/* _2_application/self-service.js — typed access to the self resource (/api/me). */
(function () {
  let repo = null;

  function init(deps) {
    repo = deps.repo;
    return { ready: true };
  }

  function getMe() { return repo.get(); }

  window.App = window.App || {};
  window.App.SelfService = {
    init: init,
    getMe: getMe,
  };
})();

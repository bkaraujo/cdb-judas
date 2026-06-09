/* _2_application/self-service.js — typed access to the self resource (/api/me). */
(function () {
  let repo = null;

  function init(deps) {
    repo = deps.repo;
    return { ready: true };
  }

  function getMe() { return repo.get(); }

  // Partial PATCH /api/me — sends only the display name; preferences stay untouched.
  function updateName(name) { return repo.patch({ name: name == null ? '' : name }); }

  window.App = window.App || {};
  window.App.SelfService = {
    init: init,
    getMe: getMe,
    updateName: updateName,
  };
})();

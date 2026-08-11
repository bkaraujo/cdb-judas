/* _2_application/system-service.js — global system resources. */
(function () {
  let api = null;

  function init(deps) {
    api = deps.api;
    return { ready: true };
  }

  function getVersion() {
    return api.global.get('/version');
  }

  window.App = window.App || {};
  window.App.SystemService = {
    init: init,
    getVersion: getVersion
  };
})();

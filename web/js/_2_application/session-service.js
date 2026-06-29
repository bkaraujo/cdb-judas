/* _2_application/session-service.js — auth + bootstrap orchestration. */
(function () {
  let authStore = null;
  let loginFn   = null;
  let hydrate   = null;
  let sse       = null;
  let onUnauthorizedCb = function () {};

  function init(deps) {
    authStore = deps.authStore;
    loginFn   = deps.loginFn;
    hydrate   = deps.hydrate;
    sse       = deps.sse;
    return { ready: true };
  }

  function isAuthenticated() { return !!authStore.get(); }

  function login(user, password) {
    return loginFn(user, password)
      .then(function (result) {
        authStore.set(result.token);
        authStore.setUser(user);
        if (result.userId) authStore.setUserId(result.userId);
        return hydrate();
      })
      .catch(function (e) {
        authStore.clear();
        throw e;
      });
  }

  function logout() {
    if (sse) sse.disconnect();
    authStore.clear();
    window.location.reload();
  }

  function bootstrap() {
    return hydrate();
  }

  function startSSE() { if (sse) sse.connect(); }
  function stopSSE()  { if (sse) sse.disconnect(); }

  function onUnauthorized(cb) {
    onUnauthorizedCb = cb || function () {};
  }

  function handleUnauthorized() {
    if (sse) sse.disconnect();
    try { onUnauthorizedCb(); } catch (e) { /* noop */ }
  }

  window.App = window.App || {};
  window.App.SessionService = {
    init: init,
    isAuthenticated: isAuthenticated,
    login: login,
    logout: logout,
    bootstrap: bootstrap,
    startSSE: startSSE,
    stopSSE: stopSSE,
    onUnauthorized: onUnauthorized,
    handleUnauthorized: handleUnauthorized,
  };
})();

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
        // A missing X-User-Id must not produce a session that "works" (valid token) but silently
        // 404s every /api/{uuid}/... call via withUser's empty-prefix fallback (http-client.js) —
        // fail loudly here instead of leaving a half-authenticated session to surface as a random
        // 404 minutes later on an unrelated feature.
        if (!result.userId) throw new Error('Login sem X-User-Id na resposta');
        authStore.set(result.token);
        authStore.setUser(user);
        authStore.setUserId(result.userId);
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

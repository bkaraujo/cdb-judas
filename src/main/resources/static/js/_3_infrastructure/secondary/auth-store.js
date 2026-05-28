/* _3_infrastructure/secondary/auth-store.js — access token storage. */
(function () {
  const TOKEN_KEY = window.Infra.Storage.KEYS.AUTH_TOKEN;
  const USER_KEY  = 'auth_user';
  const UID_KEY   = 'auth_uid';
  const ss        = window.Infra.Storage.session;

  function get()         { return ss.get(TOKEN_KEY); }
  function set(token)    { ss.set(TOKEN_KEY, token); }
  function clear()       { ss.del(TOKEN_KEY); ss.del(USER_KEY); ss.del(UID_KEY); }

  function setUser(user) { ss.set(USER_KEY, user); }
  function decodeUser()  { return ss.get(USER_KEY) || 'anonymous'; }

  function setUserId(id) { ss.set(UID_KEY, id); }
  function userId()      { return ss.get(UID_KEY); }

  window.Infra = window.Infra || {};
  window.Infra.AuthStore = {
    get: get,
    set: set,
    clear: clear,
    setUser: setUser,
    decodeUser: decodeUser,
    setUserId: setUserId,
    userId: userId,
  };
})();

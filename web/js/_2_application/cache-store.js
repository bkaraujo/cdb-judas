/* _2_application/cache-store.js — read-only access to CBD cache + subscribe.
 *
 * The cache (window.CBD) is written exclusively by cadastro-bootstrap (initial)
 * and sse-client (live updates). Pages must read through this service and react
 * to changes via `subscribe(type, cb)`.
 */
(function () {
  let bus = null;

  function init(deps) {
    bus = deps && deps.bus;
    return { ready: true };
  }

  function list(key) {
    const c = window.CBD || {};
    return Array.isArray(c[key]) ? c[key] : [];
  }

  function categories()  { return list('categories'); }
  function accounts()    { return list('accounts'); }
  function tags()        { return list('tags'); }
  function costCenters() { return list('costCenters'); }

  function findById(key, id) {
    const arr = list(key);
    for (let i = 0; i < arr.length; i++) {
      if (String(arr[i].id) === String(id)) return arr[i];
    }
    return null;
  }

  /* Subscribe to cbd:change. `typeFilter` may be a string, an array of strings,
     or null (no filter). */
  function subscribe(typeFilter, cb) {
    if (!bus) return function () {};
    const accepts = (function () {
      if (Array.isArray(typeFilter)) return function (t) { return typeFilter.indexOf(t) >= 0; };
      if (typeof typeFilter === 'string') return function (t) { return t === typeFilter; };
      return function () { return true; };
    })();
    const handler = function (_evt, detail) {
      if (!detail || !accepts(detail.type)) return;
      try { cb(detail); } catch (e) { /* noop */ }
    };
    bus.on('cbd:change', handler);
    return function () { bus.off('cbd:change', handler); };
  }

  window.App = window.App || {};
  window.App.CacheStore = {
    init: init,
    categories: categories,
    accounts: accounts,
    tags: tags,
    costCenters: costCenters,
    findById: findById,
    subscribe: subscribe,
  };
})();

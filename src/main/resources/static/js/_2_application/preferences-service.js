/* _2_application/preferences-service.js — server-owned user preferences.
 *
 * localStorage acts as a MIRROR CACHE of the server preferences so the app can apply
 * them instantly at boot (no theme flash). Every change is WRITE-THROUGH: applied to the
 * mirror immediately and pushed to the server via a debounced PATCH /api/me. Works
 * offline (kept pending) and re-syncs on reconnect. At login, applyServer() reconciles:
 * the server wins, except when the server theme is null — then the client value is taught
 * back to the server.
 *
 * Server-synced fields: theme, language, locale, sidebarCollapsed.
 * Local-only (not synced): last screen, sidebar groups, dashboard settings.
 */
(function () {
  const MIRROR_KEY = 'cbd-preferences';
  const DEBOUNCE_MS = 500;
  const DEFAULTS = { theme: null, language: 'pt-BR', locale: 'pt-BR', sidebarCollapsed: false };
  const SYNCED = ['theme', 'language', 'locale', 'sidebarCollapsed'];

  let storage = null;
  let K = null;
  let sync = function () { return Promise.resolve(); }; // function(patch) -> Promise
  let prefs = null;     // in-memory mirror
  let pending = {};     // unsynced field -> value
  let timer = null;

  function readMirror() {
    const raw = storage.local.json(MIRROR_KEY);
    return (raw && typeof raw === 'object') ? Object.assign({}, DEFAULTS, raw) : Object.assign({}, DEFAULTS);
  }
  function writeMirror() { storage.local.setJson(MIRROR_KEY, prefs); }

  function init(deps) {
    storage = deps.storage;
    K = storage.KEYS;
    if (deps.sync) sync = deps.sync;
    prefs = readMirror();
    migrateLegacy();
    if (typeof window !== 'undefined' && window.addEventListener) {
      window.addEventListener('online', function () { flush(); });
    }
    return { ready: true };
  }

  // One-time pickup of values from the pre-mirror single-key layout.
  function migrateLegacy() {
    if (prefs.theme == null) {
      const t = storage.local.get(K.THEME);
      if (t) prefs.theme = t;
    }
    const sc = storage.local.get(K.SIDEBAR_COLLAPSED);
    if (sc != null) prefs.sidebarCollapsed = (sc === '1');
    writeMirror();
  }

  function isAuthenticated() {
    return !!(window.Infra && window.Infra.AuthStore && window.Infra.AuthStore.get());
  }

  // ── Generic typed access over the synced mirror ──
  function get(key) { return prefs[key]; }

  function set(key, value) {
    if (prefs[key] === value) return;
    prefs[key] = value;
    writeMirror();
    if (SYNCED.indexOf(key) >= 0) { pending[key] = value; schedule(); }
  }

  function schedule() {
    if (timer) clearTimeout(timer);
    timer = setTimeout(function () { flush(); }, DEBOUNCE_MS);
  }

  function flush() {
    if (timer) { clearTimeout(timer); timer = null; }
    const keys = Object.keys(pending);
    if (!keys.length) return Promise.resolve();
    if (!isAuthenticated()) return Promise.resolve(); // defer; login reconciliation decides

    const patch = {};
    keys.forEach(function (k) { patch[k] = pending[k]; });
    return sync(patch)
      .then(function () {
        // Clear only fields still holding the value we sent (a newer change re-queues them).
        keys.forEach(function (k) { if (pending[k] === patch[k]) delete pending[k]; });
      })
      .catch(function () { /* offline/failed: keep pending; retry on 'online' or next change */ });
  }

  // Reconciliation at login: server wins; server theme null → adopt + teach the client value.
  function applyServer(serverPrefs) {
    if (!serverPrefs) return;
    const local = prefs || readMirror();
    pending = {}; // drop unsynced local changes — the server is the source of truth
    const merged = Object.assign({}, DEFAULTS, serverPrefs);
    if (serverPrefs.theme == null && local.theme != null) {
      merged.theme = local.theme;
      pending.theme = local.theme;
    }
    prefs = merged;
    writeMirror();
    if (Object.keys(pending).length) schedule();
  }

  // ── Theme (used by theme.js) ──
  function getTheme()            { return prefs.theme; }
  function setTheme(t)           { set('theme', t); }

  // ── Sidebar collapse (used by sidebar.js) ──
  function getSidebarCollapsed() { return !!prefs.sidebarCollapsed; }
  function setSidebarCollapsed(b){ set('sidebarCollapsed', !!b); }

  // ── Local-only (not server-synced) ──
  function getLastScreen()          { return storage.local.get(K.SCREEN); }
  function setLastScreen(id)        { storage.local.set(K.SCREEN, id); }
  function getSidebarGroups()       { return storage.local.json(K.SIDEBAR_GROUPS) || {}; }
  function setSidebarGroups(obj)    { storage.local.setJson(K.SIDEBAR_GROUPS, obj); }
  function getDashboardSettings()   { return storage.local.json(K.DASHBOARD_SETTINGS); }
  function saveDashboardSettings(s) { storage.local.setJson(K.DASHBOARD_SETTINGS, s); }

  window.App = window.App || {};
  window.App.PreferencesService = {
    init: init,
    get: get,
    set: set,
    flush: flush,
    applyServer: applyServer,
    getTheme: getTheme,
    setTheme: setTheme,
    getLastScreen: getLastScreen,
    setLastScreen: setLastScreen,
    getSidebarCollapsed: getSidebarCollapsed,
    setSidebarCollapsed: setSidebarCollapsed,
    getSidebarGroups: getSidebarGroups,
    setSidebarGroups: setSidebarGroups,
    getDashboardSettings: getDashboardSettings,
    saveDashboardSettings: saveDashboardSettings,
  };
})();

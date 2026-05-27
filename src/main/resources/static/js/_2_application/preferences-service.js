/* _2_application/preferences-service.js — typed access to user preferences. */
(function () {
  let storage = null;
  let K = null;

  function init(deps) {
    storage = deps.storage;
    K = storage.KEYS;
    return { ready: true };
  }

  function getTheme()                  { return storage.local.get(K.THEME); }
  function setTheme(t)                 { storage.local.set(K.THEME, t); }

  function getLastScreen()             { return storage.local.get(K.SCREEN); }
  function setLastScreen(id)           { storage.local.set(K.SCREEN, id); }

  function getSidebarCollapsed()       { return storage.local.get(K.SIDEBAR_COLLAPSED) === '1'; }
  function setSidebarCollapsed(b)      { storage.local.set(K.SIDEBAR_COLLAPSED, b ? '1' : '0'); }

  function getSidebarGroups()          { return storage.local.json(K.SIDEBAR_GROUPS) || {}; }
  function setSidebarGroups(obj)       { storage.local.setJson(K.SIDEBAR_GROUPS, obj); }

  function getDashboardSettings()      { return storage.local.json(K.DASHBOARD_SETTINGS); }
  function saveDashboardSettings(s)    { storage.local.setJson(K.DASHBOARD_SETTINGS, s); }

  window.App = window.App || {};
  window.App.PreferencesService = {
    init: init,
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

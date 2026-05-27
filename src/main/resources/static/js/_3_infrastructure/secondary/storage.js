/* _3_infrastructure/secondary/storage.js — typed wrappers over local/session storage. */
(function () {
  const KEYS = {
    THEME:               'cbd-theme',
    SCREEN:              'cbd-screen',
    SIDEBAR_COLLAPSED:   'cbd-sidebar-collapsed',
    SIDEBAR_GROUPS:      'cbd-sidebar-groups',
    DASHBOARD_SETTINGS:  'cbd-dashboard-settings',
    AUTH_TOKEN:          'auth_token',
  };

  function lsGet(key)         { try { return localStorage.getItem(key); } catch (e) { return null; } }
  function lsSet(key, value)  { try { localStorage.setItem(key, value); } catch (e) { /* noop */ } }
  function lsDel(key)         { try { localStorage.removeItem(key); } catch (e) { /* noop */ } }
  function lsJson(key)        {
    const raw = lsGet(key);
    if (!raw) return null;
    try { return JSON.parse(raw); } catch (e) { return null; }
  }
  function lsSetJson(key, v)  { lsSet(key, JSON.stringify(v)); }

  function ssGet(key)         { try { return sessionStorage.getItem(key); } catch (e) { return null; } }
  function ssSet(key, value)  { try { sessionStorage.setItem(key, value); } catch (e) { /* noop */ } }
  function ssDel(key)         { try { sessionStorage.removeItem(key); } catch (e) { /* noop */ } }

  window.Infra = window.Infra || {};
  window.Infra.Storage = {
    KEYS: KEYS,
    local:   { get: lsGet, set: lsSet, del: lsDel, json: lsJson, setJson: lsSetJson },
    session: { get: ssGet, set: ssSet, del: ssDel },
  };
})();

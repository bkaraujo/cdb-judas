/* _3_infrastructure/primary/theme.js — data-theme toggle, sourced from PreferencesService.
 * Reads/writes the theme via the preferences service (mirror cache + write-through PATCH),
 * not raw localStorage. Boot reads the mirror so there is no flash of the wrong theme. */

window.Theme = (function () {
  const DEFAULT = 'dark';

  function svc() { return window.App && window.App.PreferencesService; }

  function get() { return document.documentElement.getAttribute('data-theme') || DEFAULT; }

  function apply(t) {
    document.documentElement.setAttribute('data-theme', t);
    $(document).trigger('theme:change', [t]);
  }

  function set(t) {
    apply(t);
    const s = svc();
    if (s) s.setTheme(t); // write-through: mirror now, debounced PATCH /api/me
  }

  function toggle() { set(get() === 'dark' ? 'light' : 'dark'); }

  // Apply the mirror-cached theme immediately (no network wait).
  function restore() {
    const s = svc();
    const t = (s && s.getTheme()) || DEFAULT;
    apply(t);
  }

  return { get: get, set: set, toggle: toggle, restore: restore };
})();

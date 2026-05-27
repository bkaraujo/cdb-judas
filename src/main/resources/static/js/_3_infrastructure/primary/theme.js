/* _3_infrastructure/primary/theme.js — data-theme toggle + persistence. */

window.Theme = (function () {
  const KEY = 'cbd-theme';

  function get() { return document.documentElement.getAttribute('data-theme') || 'dark'; }
  function set(t) {
    document.documentElement.setAttribute('data-theme', t);
    localStorage.setItem(KEY, t);
    $(document).trigger('theme:change', [t]);
  }
  function toggle() { set(get() === 'dark' ? 'light' : 'dark'); }
  function restore() { set(localStorage.getItem(KEY) || 'dark'); }

  return { get: get, set: set, toggle: toggle, restore: restore };
})();

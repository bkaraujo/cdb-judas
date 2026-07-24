/* _2_application/period-service.js — shared period (month/year) filter.
 *
 * Local-only, session-scoped state (like "última tela"): the period the user
 * last navigated to is kept across screens so switching pages preserves the
 * filter. Canonical convention matches Domain.Period: month is 1-12.
 * Mirrored in sessionStorage so it also survives a reload within the tab; a
 * fresh session defaults to the current month.
 */
(function () {
  const KEY = 'cbd-period';
  let current = null;

  function storage() { return window.Infra && window.Infra.Storage; }

  function persist(p) {
    const st = storage();
    if (st) st.session.set(KEY, p.year + '-' + p.month);
  }

  function restore() {
    const st = storage();
    const raw = st ? st.session.get(KEY) : null;
    if (!raw) return null;
    const parts = raw.split('-');
    const year  = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10);
    if (!(month >= 1 && month <= 12) || !(year >= 1900 && year <= 9999)) return null;
    return window.Domain.Period.create(month, year);
  }

  function ensure() {
    if (current) return current;
    current = restore() || window.Domain.Period.currentMonth();
    return current;
  }

  /* Returns { month: 1-12, year }. */
  function get() { return ensure(); }

  /* Persist the navigated period. month is 1-12. */
  function set(month, year) {
    if (!(month >= 1 && month <= 12) || !(year >= 1900 && year <= 9999)) return current;
    current = window.Domain.Period.create(month, year);
    persist(current);
    return current;
  }

  window.App = window.App || {};
  window.App.PeriodService = {
    get: get,
    set: set,
  };
})();

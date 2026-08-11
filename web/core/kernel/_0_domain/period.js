/* _1_domain/period.js — month/year value object. Pure. */
(function () {
  function pad2(n) { return n < 10 ? '0' + n : '' + n; }

  function create(month, year) {
    return { month: month, year: year };
  }

  function currentMonth() {
    const d = new Date();
    return create(d.getMonth() + 1, d.getFullYear());
  }

  function shift(p, n) {
    const d = new Date(p.year, p.month - 1 + n, 1);
    return create(d.getMonth() + 1, d.getFullYear());
  }

  function bounds(p) {
    const last = new Date(p.year, p.month, 0).getDate();
    return {
      from: p.year + '-' + pad2(p.month) + '-01',
      to:   p.year + '-' + pad2(p.month) + '-' + pad2(last),
    };
  }

  /* "yyyyMM" — the period key used by the monthly-balance endpoint. */
  function yyyymm(p) {
    return '' + p.year + pad2(p.month);
  }

  function containsDate(p, dateLike) {
    const d = (dateLike instanceof Date) ? dateLike : new Date(dateLike);
    return d.getFullYear() === p.year && (d.getMonth() + 1) === p.month;
  }

  function equals(a, b) {
    return a && b && a.month === b.month && a.year === b.year;
  }

  /* "YYYY-MM" — the closing-period format used by the accounting-closing endpoint.
   * Distinct from yyyymm() above, which is the balance-endpoint format ("yyyyMM"). */
  function yyyyDashMm(p) {
    return p.year + '-' + pad2(p.month);
  }

  function fromYyyyDashMm(s) {
    if (!s) return null;
    const parts = s.split('-');
    if (parts.length !== 2) return null;
    return create(parseInt(parts[1], 10), parseInt(parts[0], 10));
  }

  window.Domain = window.Domain || {};
  window.Domain.Period = {
    create: create,
    currentMonth: currentMonth,
    shift: shift,
    bounds: bounds,
    yyyymm: yyyymm,
    yyyyDashMm: yyyyDashMm,
    fromYyyyDashMm: fromYyyyDashMm,
    containsDate: containsDate,
    equals: equals,
  };
})();

/* _1_domain/budget.js — Budget item rules. Pure. */
(function () {
  function normalize(raw) {
    if (!raw) return null;
    const budgeted = +(raw.budgeted != null ? raw.budgeted : (raw.budget != null ? raw.budget : raw.target)) || 0;
    const spent    = +(raw.spent != null ? raw.spent : raw.actual) || 0;
    return {
      id:         raw.id,
      categoryId: raw.categoryId,
      name:       raw.name || '',
      budgeted:   budgeted,
      spent:      spent,
      month:      raw.month,
      year:       raw.year,
      color:      raw.color || null,
      icon:       raw.icon || null,
    };
  }

  /* Consumption percent, clamped to [0, 100]. */
  function consumptionPct(spent, budgeted) {
    const b = +budgeted || 0;
    if (b <= 0) return 0;
    return Math.min(100, Math.max(0, (Math.abs(+spent || 0) / b) * 100));
  }

  function isOverBudget(spent, budgeted) {
    const b = +budgeted || 0;
    return b > 0 && Math.abs(+spent || 0) > b;
  }

  /* Returns { value, over } where `value` is the remaining (positive) or
     the over-spent amount (positive) and `over` flags which one. */
  function remainingOrOver(spent, budgeted) {
    const b = +budgeted || 0;
    const s = Math.abs(+spent || 0);
    if (s > b) return { value: s - b, over: true };
    return { value: b - s, over: false };
  }

  /* Bar color tokens by consumption percent. Mirrors STYLE.md §11. */
  function barColor(pct) {
    if (pct >= 80) return 'expense';
    if (pct >= 60) return 'warning';
    return 'accent';
  }

  function overspendCount(items) {
    return (items || []).reduce(function (acc, b) {
      return acc + (isOverBudget(b.spent, b.budgeted) ? 1 : 0);
    }, 0);
  }

  window.Domain = window.Domain || {};
  window.Domain.Budget = {
    normalize: normalize,
    consumptionPct: consumptionPct,
    isOverBudget: isOverBudget,
    remainingOrOver: remainingOrOver,
    barColor: barColor,
    overspendCount: overspendCount,
  };
})();

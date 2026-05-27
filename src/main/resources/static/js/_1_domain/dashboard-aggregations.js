/* _1_domain/dashboard-aggregations.js — pure analytics for dashboard panels. */
(function () {
  function txIsExpense(t, categoryNature) {
    if (!t) return false;
    if (t.type) {
      const s = String(t.type).toLowerCase();
      if (s === 'expense') return true;
      if (s === 'income' || s === 'revenue') return false;
    }
    if (categoryNature) return String(categoryNature).toUpperCase() === 'EXPENSE';
    return (+t.amount || 0) < 0;
  }

  function categoryNameFor(t, categories) {
    if (!t || t.categoryId == null) return 'Sem categoria';
    const c = window.Domain.Category.byId(categories, t.categoryId);
    return c ? c.name : 'Sem categoria';
  }

  function categoryNatureFor(t, categories) {
    if (!t || t.categoryId == null) return null;
    const c = window.Domain.Category.byId(categories, t.categoryId);
    return c ? c.nature : null;
  }

  /* Sum |amount| per category name across expense transactions. */
  function expenseByCategory(transactions, categories) {
    const out = {};
    (transactions || []).forEach(function (t) {
      const nature = categoryNatureFor(t, categories);
      if (!txIsExpense(t, nature)) return;
      const name = categoryNameFor(t, categories);
      out[name] = (out[name] || 0) + Math.abs(+t.amount || 0);
    });
    const arr = Object.keys(out).map(function (k) { return { name: k, value: out[k] }; });
    arr.sort(function (a, b) { return b.value - a.value; });
    return arr;
  }

  function topN(items, n) {
    return (items || []).slice(0, n || 5);
  }

  /* Monthly receitas/despesas series over the last `monthsBack` months. */
  function monthlySeries(transactions, monthsBack, now) {
    const m = monthsBack || 4;
    const ref = now || new Date();
    const buckets = [];
    for (let i = m - 1; i >= 0; i--) {
      const d = new Date(ref.getFullYear(), ref.getMonth() - i, 1);
      buckets.push({ month: d.getMonth() + 1, year: d.getFullYear(), receitas: 0, despesas: 0 });
    }
    (transactions || []).forEach(function (t) {
      const d = new Date(t.date);
      const idx = buckets.findIndex(function (b) {
        return b.month === d.getMonth() + 1 && b.year === d.getFullYear();
      });
      if (idx < 0) return;
      const v = Math.abs(+t.amount || 0);
      if (txIsExpense(t)) buckets[idx].despesas += v;
      else                buckets[idx].receitas += v;
    });
    return buckets;
  }

  /* Filter & sort upcoming active payables by due/date ascending. */
  function upcomingPayables(payables) {
    return (payables || [])
      .filter(window.Domain.Payable.isActive)
      .slice()
      .sort(function (a, b) {
        const da = new Date(a.due || a.date || 0).getTime();
        const db = new Date(b.due || b.date || 0).getTime();
        return da - db;
      });
  }

  window.Domain = window.Domain || {};
  window.Domain.DashboardAggregations = {
    txIsExpense: txIsExpense,
    categoryNameFor: categoryNameFor,
    categoryNatureFor: categoryNatureFor,
    expenseByCategory: expenseByCategory,
    topN: topN,
    monthlySeries: monthlySeries,
    upcomingPayables: upcomingPayables,
  };
})();

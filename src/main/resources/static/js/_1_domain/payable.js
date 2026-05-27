/* _1_domain/payable.js — Payable / Receivable rules. Pure. */
(function () {
  const TYPES  = { PAYABLE: 'PAYABLE', RECEIVABLE: 'RECEIVABLE' };
  const STATUS = {
    PENDING:   'pending',
    SCHEDULED: 'scheduled',
    CONFIRMED: 'confirmed',
    CANCELLED: 'cancelled',
  };

  function normalize(raw) {
    if (!raw) return null;
    return {
      id:         raw.id,
      name:       raw.name || raw.description || '',
      amount:     +raw.amount || 0,
      due:        raw.due || raw.date || null,
      status:     String(raw.status || STATUS.PENDING).toLowerCase(),
      categoryId: raw.categoryId,
      accountId:  raw.accountId,
      type:       String(raw.type || TYPES.PAYABLE).toUpperCase(),
    };
  }

  function isReceivable(p) { return !!p && p.type === TYPES.RECEIVABLE; }
  function isPayable(p)    { return !!p && p.type === TYPES.PAYABLE; }
  function isActive(p)     { return !!p && p.status !== STATUS.CANCELLED; }

  /* Transaction nature derived from payable type. */
  function natureOf(typeOrPayable) {
    const t = typeof typeOrPayable === 'string' ? typeOrPayable : (typeOrPayable && typeOrPayable.type);
    return String(t || '').toUpperCase() === TYPES.RECEIVABLE ? 'income' : 'expense';
  }

  /* Filter list by due date inside a Period. */
  function inPeriod(items, period) {
    return (items || []).filter(function (p) {
      if (!p.due) return false;
      return window.Domain.Period.containsDate(period, p.due);
    });
  }

  function periodTotals(items, period) {
    const inP = inPeriod(items, period);
    let payable = 0, receivable = 0;
    for (let i = 0; i < inP.length; i++) {
      const p = inP[i];
      if (!isActive(p)) continue;
      const v = Math.abs(p.amount);
      if (isReceivable(p)) receivable += v; else payable += v;
    }
    return { payable: payable, receivable: receivable, result: receivable - payable };
  }

  function statusBadgeVariant(status) {
    const s = String(status || '').toLowerCase();
    if (s === STATUS.CONFIRMED) return 'income';
    if (s === STATUS.SCHEDULED) return 'warning';
    if (s === STATUS.CANCELLED) return 'muted';
    return 'expense';
  }

  window.Domain = window.Domain || {};
  window.Domain.Payable = {
    TYPES: TYPES,
    STATUS: STATUS,
    normalize: normalize,
    isReceivable: isReceivable,
    isPayable: isPayable,
    isActive: isActive,
    natureOf: natureOf,
    inPeriod: inPeriod,
    periodTotals: periodTotals,
    statusBadgeVariant: statusBadgeVariant,
  };
})();

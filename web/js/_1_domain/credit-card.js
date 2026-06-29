/* _1_domain/credit-card.js — credit card rules (uses Account + Period). Pure. */
(function () {
  const DEFAULT_CLOSING_DAY = 1;
  const DEFAULT_DUE_DAY     = 10;

  function pad2(n) { return n < 10 ? '0' + n : '' + n; }

  function closingDay(card) {
    const d = card && card.additionalInfo && +card.additionalInfo.closingDay;
    return d > 0 ? d : DEFAULT_CLOSING_DAY;
  }

  function dueDay(card) {
    const d = card && card.additionalInfo && +card.additionalInfo.dueDay;
    return d > 0 ? d : DEFAULT_DUE_DAY;
  }

  /* Invoice period for a given month/year. Currently matches the calendar month
     (consistent with the existing credit-cards.js:43 behavior). */
  function invoicePeriod(period /*, card */) {
    return window.Domain.Period.bounds(period);
  }

  function usagePct(used, limit) {
    const u = Math.abs(+used || 0);
    const l = +limit || 0;
    if (l <= 0) return 0;
    return Math.min(100, Math.max(0, (u / l) * 100));
  }

  function availableCredit(limit, used) {
    return Math.max(0, (+limit || 0) - Math.abs(+used || 0));
  }

  /* Bar color tokens by usage percent. Mirrors STYLE.md §11. */
  function barColorByUsage(pct) {
    if (pct >= 80) return 'expense';
    if (pct >= 60) return 'warning';
    return 'accent';
  }

  /* Total invoice = sum of |amount| over EXPENSE transactions inside the period
     that belong to the card account. */
  function invoiceTotal(transactions, cardId, period) {
    const b = invoicePeriod(period);
    return (transactions || []).reduce(function (acc, t) {
      if (String(t.accountId) !== String(cardId)) return acc;
      const dStr = String(t.date || '').slice(0, 10);
      if (dStr < b.from || dStr > b.to) return acc;
      const isExpense = String(t.type || '').toUpperCase() === 'EXPENSE' || (+t.amount || 0) < 0;
      if (!isExpense) return acc;
      return acc + Math.abs(+t.amount || 0);
    }, 0);
  }

  window.Domain = window.Domain || {};
  window.Domain.CreditCard = {
    DEFAULT_CLOSING_DAY: DEFAULT_CLOSING_DAY,
    DEFAULT_DUE_DAY:     DEFAULT_DUE_DAY,
    closingDay:          closingDay,
    dueDay:              dueDay,
    invoicePeriod:       invoicePeriod,
    usagePct:            usagePct,
    availableCredit:     availableCredit,
    barColorByUsage:     barColorByUsage,
    invoiceTotal:        invoiceTotal,
  };
})();

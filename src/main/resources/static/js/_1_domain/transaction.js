/* _1_domain/transaction.js — Transaction entity + rules. Pure. */
(function () {
  const TYPES = { INCOME: 'income', EXPENSE: 'expense', TRANSFER: 'transfer' };
  const STATUS = {
    PENDING:   'pending',
    SCHEDULED: 'scheduled',
    CONFIRMED: 'confirmed',
    PLANNED:   'planed',
    BALANCE:   'balance',
    CANCELLED: 'cancelled',
  };
  const GROUP_SCOPE = { SINGLE: 'SINGLE', FUTURE: 'FUTURE', ALL: 'ALL' };

  function normalizeType(t) {
    const s = String(t || '').toLowerCase();
    if (s === 'income' || s === 'revenue') return TYPES.INCOME;
    if (s === 'transfer') return TYPES.TRANSFER;
    return TYPES.EXPENSE;
  }

  function isExpense(t)  { return normalizeType(t && t.type) === TYPES.EXPENSE; }
  function isIncome(t)   { return normalizeType(t && t.type) === TYPES.INCOME; }
  function isTransfer(t) { return normalizeType(t && t.type) === TYPES.TRANSFER; }

  /* Signed amount: expense → negative; income/transfer → positive. */
  function signedAmount(type, amount) {
    const v = Math.abs(+amount || 0);
    return normalizeType(type) === TYPES.EXPENSE ? -v : v;
  }

  function statusBadgeVariant(status) {
    const s = String(status || '').toLowerCase();
    if (s === STATUS.CONFIRMED) return 'income';
    if (s === STATUS.SCHEDULED) return 'warning';
    if (s === STATUS.PLANNED)   return 'info';
    if (s === STATUS.CANCELLED) return 'muted';
    return 'expense'; // pending or unknown
  }

  /* Transfer rule: source account differs from destination. */
  function isValidTransfer(srcAccountId, dstAccountId) {
    if (srcAccountId == null || dstAccountId == null) return false;
    return String(srcAccountId) !== String(dstAccountId);
  }

  function isToday(t, now) {
    const ref = now || new Date();
    const d = new Date(t.date);
    return d.getFullYear() === ref.getFullYear()
        && d.getMonth() === ref.getMonth()
        && d.getDate() === ref.getDate();
  }

  function isInPeriod(t, period) {
    return window.Domain.Period.containsDate(period, t.date);
  }

  window.Domain = window.Domain || {};
  window.Domain.Transaction = {
    TYPES: TYPES,
    STATUS: STATUS,
    GROUP_SCOPE: GROUP_SCOPE,
    normalizeType: normalizeType,
    isExpense: isExpense,
    isIncome: isIncome,
    isTransfer: isTransfer,
    signedAmount: signedAmount,
    statusBadgeVariant: statusBadgeVariant,
    isValidTransfer: isValidTransfer,
    isToday: isToday,
    isInPeriod: isInPeriod,
  };
})();

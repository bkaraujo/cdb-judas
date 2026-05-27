/* _1_domain/statement-item.js — statement row helpers. Pure. */
(function () {
  const STATUS = { BALANCE: 'balance', CONFIRMED: 'confirmed' };

  function isBalanceHeader(rowOrStatus) {
    const s = typeof rowOrStatus === 'string' ? rowOrStatus : (rowOrStatus && rowOrStatus.status);
    return String(s || '').toLowerCase() === STATUS.BALANCE;
  }

  /* Running balance: prefer `runningBal`, fall back to `balance`. */
  function runningBalance(row) {
    if (!row) return 0;
    if (row.runningBal != null) return +row.runningBal;
    if (row.balance != null)    return +row.balance;
    return 0;
  }

  window.Domain = window.Domain || {};
  window.Domain.StatementItem = {
    STATUS: STATUS,
    isBalanceHeader: isBalanceHeader,
    runningBalance: runningBalance,
  };
})();

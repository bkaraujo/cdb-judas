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

  /* Builds the statement rows for a period: a "Saldo anterior" balance-header row carrying
     `opening`, then each transaction (all statuses, sorted ascending by date) with its
     cumulative running balance. Empty `txs` yields the opening row alone. Pure — no I/O. */
  function buildRows(opening, txs, openingDate) {
    const open = +opening || 0;
    const rows = [{
      id: null,
      date: openingDate || null,
      description: 'Saldo anterior',
      amount: 0,
      status: STATUS.BALANCE,
      runningBal: open,
      categoryId: null,
    }];
    let running = open;
    (txs || []).slice()
      .sort(function (a, b) { return String(a.date).localeCompare(String(b.date)); })
      .forEach(function (t) {
        running += (+t.amount || 0);
        rows.push({
          id: t.id,
          date: t.date,
          description: t.description,
          amount: +t.amount || 0,
          status: t.status,
          runningBal: running,
          categoryId: t.categoryId,
        });
      });
    return rows;
  }

  window.Domain = window.Domain || {};
  window.Domain.StatementItem = {
    STATUS: STATUS,
    isBalanceHeader: isBalanceHeader,
    runningBalance: runningBalance,
    buildRows: buildRows,
  };
})();

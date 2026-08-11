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
     cumulative running balance. Empty `txs` yields the opening row alone. Pure — no I/O.
     `opts` (opcional) atende o extrato de cartão, onde o cabeçalho mostra o total da fatura
     ANTERIOR mas o acumulado do ciclo atual recomeça do zero:
       { headerLabel: 'Fatura anterior', headerBalance: <total anterior>, startBalance: 0 } */
  function buildRows(opening, txs, openingDate, opts) {
    const o = opts || {};
    const open = +opening || 0;
    const headerBalance = o.headerBalance != null ? +o.headerBalance : open;
    const rows = [{
      id: null,
      date: openingDate || null,
      description: o.headerLabel || 'Saldo anterior',
      amount: 0,
      status: STATUS.BALANCE,
      runningBal: headerBalance,
      categoryId: null,
    }];
    let running = o.startBalance != null ? +o.startBalance : open;
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
          // Propagados para a view montar o link da fatura (statement.js) e resolver o cartão.
          cardId: t.cardId != null ? t.cardId : null,
          invoice: t.invoice === true,
          // Parcela: alimenta Domain.Transaction.describe na linha do extrato.
          installmentNumber: t.installmentNumber != null ? t.installmentNumber : null,
          totalInstallments: t.totalInstallments != null ? t.totalInstallments : null,
          // Vínculos de tag: alimentam o indicador de tags da linha (UI.tagFlagHtml).
          tagIds: Array.isArray(t.tagIds) ? t.tagIds : [],
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

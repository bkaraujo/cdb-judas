/* _1_domain/balance-sheet.js — balance sheet computation. Pure. */
(function () {
  function compute(accounts) {
    let assets = 0, liabilities = 0;
    (accounts || []).forEach(function (a) {
      const bal = window.Domain.Account.currentBalance(a);
      if (window.Domain.Account.isLiability(a)) {
        liabilities += Math.abs(bal);
      } else {
        assets += bal;
      }
    });
    return { assets: assets, liabilities: liabilities, equity: assets - liabilities };
  }

  window.Domain = window.Domain || {};
  window.Domain.BalanceSheet = { compute: compute };
})();

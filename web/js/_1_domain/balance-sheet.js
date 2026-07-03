/* _1_domain/balance-sheet.js — balance sheet computation. Pure. */
(function () {
  // No account type is a liability post-card-remodel (cards are no longer accounts) —
  // everything sits on the ATIVO side until the model grows a real liability account type.
  function compute(accounts) {
    let assets = 0;
    (accounts || []).forEach(function (a) {
      assets += window.Domain.Account.currentBalance(a);
    });
    return { assets: assets, liabilities: 0, equity: assets };
  }

  window.Domain = window.Domain || {};
  window.Domain.BalanceSheet = { compute: compute };
})();

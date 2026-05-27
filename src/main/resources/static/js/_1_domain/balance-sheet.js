/* _1_domain/balance-sheet.js — balance sheet computation. Pure. */
(function () {
  function compute(accounts) {
    let ativo = 0, passivo = 0;
    (accounts || []).forEach(function (a) {
      if (window.Domain.Account.isLiability(a)) {
        passivo += Math.abs(+a.balance || 0);
      } else {
        ativo += (+a.balance || 0);
      }
    });
    return { ativo: ativo, passivo: passivo, patrimonio: ativo - passivo };
  }

  window.Domain = window.Domain || {};
  window.Domain.BalanceSheet = { compute: compute };
})();

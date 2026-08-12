/* feature/accounts-payable.api.js — contrato público da fatia accounts-payable (equivalente ao
 * FNNNApi do backend). Único arquivo que outra fatia pode referenciar. Consumidor: dashboard
 * (painéis de resultado/próximos vencimentos — fecha V6). */
(function () {
  window.AccountsPayableApi = {
    listPayable:    function () { return window.App.PayableService.listPayable(); },
    listReceivable: function () { return window.App.PayableService.listReceivable(); },
    isActive:       function (item) { return window.Domain.Payable.isActive(item); },
  };
})();

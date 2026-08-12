/* feature/credit-cards.api.js — contrato público da fatia credit-cards (equivalente ao FNNNApi
 * do backend). Único arquivo que outra fatia pode referenciar. Consumidor: dashboard (painel de
 * cartões de crédito — fecha V7). */
(function () {
  window.CreditCardsApi = {
    accountsWithCards:   function () { return window.App.CreditCardService.accountsWithCards(); },
    usagePct:            function (used, limit) { return window.Domain.CreditCard.usagePct(used, limit); },
    barColorByUsage:     function (pct) { return window.Domain.CreditCard.barColorByUsage(pct); },
    accountInvoiceTotal: function (transactions, account, period) { return window.Domain.CreditCard.accountInvoiceTotal(transactions, account, period); },
  };
})();

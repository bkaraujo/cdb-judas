/* feature/transactions.api.js — contrato público da fatia transactions (equivalente ao FNNNApi
 * do backend). Único arquivo que outra fatia pode referenciar. Consumidores: statement,
 * card-statement (editor/exclusão/marcar-pago de uma linha — V1), credit-cards, statement,
 * accounts-payable, dashboard (leitura de transações no lugar do repositório cru — V2/V3/V5/V8). */
(function () {
  window.TransactionsApi = {
    list:           function (params) { return window.App.TransactionService.list(params); },
    listByAccount:  function (accountId, params) { return window.App.TransactionService.listByAccount(accountId, params); },
    openEditor:     function (opts) { return window.transactionActions.openFormModal(opts); },
    openDeleteFlow: function (tx, opts) { return window.transactionActions.openDeleteModal(tx, opts); },
    markPaid:       function (tx, opts) { return window.transactionActions.markPaid(tx, opts); },
  };
})();

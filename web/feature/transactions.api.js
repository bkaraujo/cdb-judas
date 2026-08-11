/* feature/transactions.api.js — contrato público da fatia transactions (equivalente ao FNNNApi
 * do backend). Único arquivo que outra fatia pode referenciar. Consumidores: statement,
 * card-statement (editor/exclusão/marcar-pago de uma linha — V1), credit-cards, statement,
 * accounts-payable, dashboard (leitura de transações no lugar do repositório cru — V2/V3/V5/V8).
 * accounts-payable também escreve (uma conta a pagar/receber É uma transação com status
 * pendente/agendado/confirmado) — create/update/remove/patchStatus expostos pelo mesmo motivo.
 * import-statement usa importPreview/importConfirm (fecha o lado transactions do V10/V11). */
(function () {
  window.TransactionsApi = {
    list:           function (params) { return window.App.TransactionService.list(params); },
    listByAccount:  function (accountId, params) { return window.App.TransactionService.listByAccount(accountId, params); },
    create:         function (data) { return window.App.TransactionService.create(data); },
    update:         function (id, data) { return window.App.TransactionService.update(id, data); },
    remove:         function (accountId, id, scope) { return window.App.TransactionService.remove(accountId, id, scope); },
    patchStatus:    function (accountId, id, status, date) { return window.App.TransactionService.patchStatus(accountId, id, status, date); },
    openEditor:     function (opts) { return window.transactionActions.openFormModal(opts); },
    openDeleteFlow: function (tx, opts) { return window.transactionActions.openDeleteModal(tx, opts); },
    markPaid:       function (tx, opts) { return window.transactionActions.markPaid(tx, opts); },
    importPreview:  function (file, password, accountId) { return window.App.TransactionService.importPreview(file, password, accountId); },
    importConfirm:  function (payload) { return window.App.TransactionService.importConfirm(payload); },
  };
})();

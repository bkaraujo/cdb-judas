/* feature/import-statement.api.js — contrato público da fatia import-statement (equivalente ao
 * FNNNApi do backend). Único arquivo que outra fatia pode referenciar. Consumidor: transactions
 * (botão "Importar" da página de lançamentos). */
(function () {
  window.ImportStatementApi = {
    open: function (opts) { return window.importStatementModal(opts); },
  };
})();

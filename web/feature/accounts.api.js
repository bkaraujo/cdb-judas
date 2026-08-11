/* feature/accounts.api.js — contrato público da fatia accounts (equivalente ao FNNNApi do
 * backend). Único arquivo que outra fatia pode referenciar. Consumidores: statement (painel de
 * saldo por conta no período — V4), credit-cards (re-render ao mudar cartões de uma conta). */
(function () {
  window.AccountsApi = {
    // Mesmo nome de método que App.BalanceService.allAccounts — statement injeta AccountsApi
    // no lugar do serviço cru, sem precisar adaptar a chamada interna.
    allAccounts: function (period) { return window.App.BalanceService.allAccounts(period); },
    onChange:    function (cb) { return window.App.AccountService.onChange(cb); },
  };
})();

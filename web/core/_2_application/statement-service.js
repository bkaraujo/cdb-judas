/* _2_application/statement-service.js — Extrato orchestration.
 * Assembles statement rows client-side from the existing transactions + monthly-balance
 * endpoints (no /statements backend). Stable interface: load(accountId, period) / summary(period). */
(function () {
  let txRepo = null;
  let balance = null;
  let cache = null;

  function init(deps) {
    txRepo = deps.txRepo;
    balance = deps.balance;
    cache = deps.cache;
    return { ready: true };
  }

  function checkings() {
    return cache.accounts();
  }

  /* Detail for one account in a period: a "Saldo anterior" row carrying the opening
     balance (previous period's closing, resolved by summary() — no balance fetch here),
     then each transaction (all statuses) with its running balance.
     Compras de cartão não entram linha a linha: Domain.Invoice.collapse as troca por uma linha
     por (cartão, vencimento), lançada na data de vencimento — o mesmo mês em que o backend as
     debita no F002_BALANCE. Como o ciclo da fatura começa no mês anterior, a busca usa a janela
     alargada de Domain.Invoice.fetchWindow, não os bounds do período. */
  function load(accountId, period, openingBalance) {
    const account = cache.findById('accounts', accountId);
    if (!account) return Promise.resolve([]);
    const b = window.Domain.Period.bounds(period);
    const w = window.Domain.Invoice.fetchWindow([account], period);
    return txRepo.listByAccount(accountId, 'dateFrom=' + w.from + '&dateTo=' + w.to)
      .then(function (txs) {
        const rows = window.Domain.Invoice.collapse(Array.isArray(txs) ? txs : [], [account], period);
        return window.Domain.StatementItem.buildRows(openingBalance, rows, b.from);
      });
  }

  /* Left-column panorama: closing + opening (previous period's closing) balance per
     checking account for the period. Two batch requests total (GET /accounts/balance for
     the period and for period-1) instead of one per account — fetched once per period
     (mount + prev/next), never per account selection; the statement screen reuses this
     cache instead of re-fetching. Accounts absent from a batch response (no snapshot yet)
     fall back to the account's current/initial balance. */
  function summary(period) {
    const prevPeriod = window.Domain.Period.shift(period, -1);
    function byAccountId(list) {
      const map = {};
      (Array.isArray(list) ? list : []).forEach(function (b) { map[String(b.accountId)] = +b.balance || 0; });
      return map;
    }
    return Promise.all([
      balance.allAccounts(period),
      balance.allAccounts(prevPeriod),
    ]).then(function (res) {
      const closingByAccount = byAccountId(res[0]);
      const openingByAccount = byAccountId(res[1]);
      return checkings().map(function (a) {
        const id = String(a.id);
        return {
          accountId: a.id,
          closingBalance: id in closingByAccount ? closingByAccount[id] : window.Domain.Account.currentBalance(a),
          openingBalance: id in openingByAccount ? openingByAccount[id] : (+a.balance || 0),
        };
      });
    });
  }

  window.App = window.App || {};
  window.App.StatementService = {
    init: init,
    load: load,
    summary: summary,
  };
})();

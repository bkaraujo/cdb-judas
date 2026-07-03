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

  /* Opening balance = previous month's closing snapshot; on 404 (no snapshot for a month
     before the account's first activity) fall back to the account's initial balance. */
  function opening(account, period) {
    return balance.monthly(account.id, window.Domain.Period.shift(period, -1))
      .then(function (b) { return b ? (+b.balance || 0) : (+account.balance || 0); })
      .catch(function (err) {
        if (err && err.status === 404) return +account.balance || 0;
        throw err;
      });
  }

  /* Detail for one account in a period: a "Saldo anterior" row carrying the opening
     balance, then each transaction (all statuses) with its running balance. */
  function load(accountId, period) {
    const account = cache.findById('accounts', accountId);
    if (!account) return Promise.resolve([]);
    const b = window.Domain.Period.bounds(period);
    return Promise.all([
      txRepo.listByAccount(accountId, 'dateFrom=' + b.from + '&dateTo=' + b.to),
      opening(account, period),
    ]).then(function (res) {
      const txs = Array.isArray(res[0]) ? res[0] : [];
      return window.Domain.StatementItem.buildRows(res[1], txs, b.from);
    });
  }

  /* Left-column panorama: closing balance per checking account for the period.
     One balance read per account; 404 falls back to the account's current balance. */
  function summary(period) {
    return Promise.all(checkings().map(function (a) {
      return balance.monthly(a.id, period)
        .then(function (b) {
          return { accountId: a.id, closingBalance: b ? (+b.balance || 0) : window.Domain.Account.currentBalance(a) };
        })
        .catch(function (err) {
          if (err && err.status === 404) return { accountId: a.id, closingBalance: window.Domain.Account.currentBalance(a) };
          throw err;
        });
    }));
  }

  window.App = window.App || {};
  window.App.StatementService = {
    init: init,
    load: load,
    summary: summary,
  };
})();

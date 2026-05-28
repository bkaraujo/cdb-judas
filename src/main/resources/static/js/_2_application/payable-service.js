/* _2_application/payable-service.js — "A Pagar / A Receber" como filtro de transações pendentes.
 * Não há recurso de payables: A Pagar = despesas pendentes; A Receber = receitas pendentes. */
(function () {
  let txRepo = null;

  function init(deps) { txRepo = deps.repo; return { ready: true }; }

  // Adapta a transação ao formato consumido pela tela (Domain.Payable): due, amount positivo, type label.
  function adapt(label) {
    return function (txs) {
      return (Array.isArray(txs) ? txs : []).map(function (t) {
        return {
          id: t.id,
          description: t.description,
          due: t.date,
          amount: Math.abs(+t.amount || 0),
          accountId: t.accountId,
          categoryId: t.categoryId,
          status: t.status,
          type: label,
        };
      });
    };
  }

  function listPayable()    { return txRepo.list('status=pending&type=expense').then(adapt('PAYABLE')); }
  function listReceivable() { return txRepo.list('status=pending&type=income').then(adapt('RECEIVABLE')); }

  function periodTotals(items, period) { return window.Domain.Payable.periodTotals(items, period); }
  function inPeriod(items, period)     { return window.Domain.Payable.inPeriod(items, period); }

  window.App = window.App || {};
  window.App.PayableService = {
    init: init,
    listPayable: listPayable,
    listReceivable: listReceivable,
    periodTotals: periodTotals,
    inPeriod: inPeriod,
  };
})();

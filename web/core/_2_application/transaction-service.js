/* _2_application/transaction-service.js — Transaction use cases. */
(function () {
  let repo = null;
  let cache = null;

  function init(deps) { repo = deps.repo; cache = deps.cache; return { ready: true }; }

  function list(params)                    { return repo.list(params); }

  /* Lançamentos do período com as compras de cartão colapsadas em uma linha por (cartão,
     vencimento) — ver Domain.Invoice. A busca usa a janela alargada (o ciclo da fatura começa no
     mês anterior), por isso `raw` traz mais do que o período: é o índice cru que os modais de
     editar/excluir usam para resolver a transação por trás de uma linha. */
  function listForPeriod(period) {
    const accounts = cache.accounts();
    const w = window.Domain.Invoice.fetchWindow(accounts, period);
    return repo.list('dateFrom=' + w.from + '&dateTo=' + w.to).then(function (list) {
      const raw = Array.isArray(list) ? list : [];
      return { rows: window.Domain.Invoice.collapse(raw, accounts, period), raw: raw };
    });
  }
  function listByAccount(accountId, params){ return repo.listByAccount(accountId, params); }
  function create(data)                    { return repo.create(data); }
  function update(id, data)                { return repo.update(id, data); }
  function patchStatus(accountId, id, status, date) { return repo.patchStatus(accountId, id, status, date); }
  function remove(accountId, id, scope)    { return repo.remove(accountId, id, scope); }
  function transfer(data)                  { return repo.transfer(data); }
  function importPreview(file, password, accountId) { return repo.importPreview(file, password, accountId); }
  function importConfirm(data)             { return repo.importConfirm(data); }

  /* Builds payload with the domain signing rule applied to amount. */
  function buildPayload(form) {
    const type = window.Domain.Transaction.normalizeType(form.type);
    return Object.assign({}, form, {
      type: type,
      amount: window.Domain.Transaction.signedAmount(type, form.amount),
    });
  }

  window.App = window.App || {};
  window.App.TransactionService = {
    init: init,
    list: list,
    listForPeriod: listForPeriod,
    listByAccount: listByAccount,
    create: create,
    update: update,
    patchStatus: patchStatus,
    remove: remove,
    transfer: transfer,
    importPreview: importPreview,
    importConfirm: importConfirm,
    buildPayload: buildPayload,
  };
})();

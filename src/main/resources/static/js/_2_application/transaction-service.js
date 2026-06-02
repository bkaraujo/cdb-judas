/* _2_application/transaction-service.js — Transaction use cases. */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  function list(params)                    { return repo.list(params); }
  function listByAccount(accountId, params){ return repo.listByAccount(accountId, params); }
  function create(data)                    { return repo.create(data); }
  function update(id, data)                { return repo.update(id, data); }
  function patchStatus(accountId, id, status, date) { return repo.patchStatus(accountId, id, status, date); }
  function remove(accountId, id, scope)    { return repo.remove(accountId, id, scope); }
  function transfer(data)                  { return repo.transfer(data); }
  function importPreview(file, password, accountId) { return repo.importPreview(file, password, accountId); }
  function importConfirm(data)             { return repo.importConfirm(data); }
  function importStatementConfirm(data)    { return repo.importStatementConfirm(data); }

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
    listByAccount: listByAccount,
    create: create,
    update: update,
    patchStatus: patchStatus,
    remove: remove,
    transfer: transfer,
    importPreview: importPreview,
    importConfirm: importConfirm,
    importStatementConfirm: importStatementConfirm,
    buildPayload: buildPayload,
  };
})();

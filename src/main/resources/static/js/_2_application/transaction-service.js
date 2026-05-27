/* _2_application/transaction-service.js — Transaction use cases. */
(function () {
  let repo = null;

  function init(deps) { repo = deps.repo; return { ready: true }; }

  function list(params)                    { return repo.list(params); }
  function create(data)                    { return repo.create(data); }
  function update(id, data)                { return repo.update(id, data); }
  function patchStatus(id, status, date)   { return repo.patchStatus(id, status, date); }
  function remove(id, scope)               { return repo.remove(id, scope); }
  function transfer(data)                  { return repo.transfer(data); }
  function importPreview(file, password)   { return repo.importPreview(file, password); }
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

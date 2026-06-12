/* _3_infrastructure/secondary/transaction-repository.js — HTTP adapter for transactions
 * under the accounts namespace: /api/{uuid}/accounts/... */
(function () {
  function create(http) {
    return {
      list:        function (params) { return http.get('/accounts/transactions' + (params ? '?' + params : '')); },
      listByAccount: function (accountId, params) {
        return http.get('/accounts/' + accountId + '/transactions' + (params ? '?' + params : ''));
      },
      create:      function (data)   { return http.post('/accounts/' + data.accountId + '/transactions', data); },
      update:      function (id, d)  { return http.patch('/accounts/' + d.accountId + '/transactions/' + id, d); },
      patchStatus: function (accountId, id, status, paymentDate) {
        return http.patch('/accounts/' + accountId + '/transactions/' + id + '/status', { status: status, paymentDate: paymentDate });
      },
      remove:      function (accountId, id, mode) {
        return http.delete('/accounts/' + accountId + '/transactions/' + id + (mode ? '?mode=' + mode : ''));
      },
      transfer:    function (data)   { return http.post('/accounts/transactions/transfer', data); },
      importPreview: function (file, password, accountId) {
        const fd = new FormData();
        fd.append('file', file);
        if (password) fd.append('password', password);
        if (accountId) fd.append('accountId', accountId);
        return http.upload('/accounts/transactions/import/preview', fd);
      },
      importConfirm: function (data) { return http.post('/accounts/transactions/import/confirm', data); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.TransactionRepository = { create: create };
})();

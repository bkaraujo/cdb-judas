/* _3_infrastructure/secondary/transaction-repository.js — HTTP adapter for transactions
 * under the accounts namespace: /api/{uuid}/accounts/... */
(function () {
  function create(http) {
    return {
      list:        function (params) { return http.user.get('/accounts/transactions' + (params ? '?' + params : '')); },
      listByAccount: function (accountId, params) {
        return http.user.get('/accounts/' + accountId + '/transactions' + (params ? '?' + params : ''));
      },
      create:      function (data)   { return http.user.post('/accounts/' + data.accountId + '/transactions', data); },
      update:      function (id, d)  { return http.user.patch('/accounts/' + d.accountId + '/transactions/' + id, d); },
      patchStatus: function (accountId, id, status, paymentDate) {
        return http.user.patch('/accounts/' + accountId + '/transactions/' + id + '/status', { status: status, paymentDate: paymentDate });
      },
      remove:      function (accountId, id, mode) {
        return http.user.delete('/accounts/' + accountId + '/transactions/' + id + (mode ? '?mode=' + mode : ''));
      },
      transfer:    function (data)   { return http.user.post('/accounts/transactions/transfer', data); },
      importPreview: function (file, password) {
        const fd = new FormData();
        fd.append('file', file);
        if (password) fd.append('password', password);
        return http.user.upload('/accounts/transactions/import/preview', fd);
      },
      importConfirm: function (data) { return http.user.post('/accounts/transactions/import/confirm', data); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.TransactionRepository = { create: create };
})();

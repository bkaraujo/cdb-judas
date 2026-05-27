/* _3_infrastructure/secondary/transaction-repository.js — HTTP adapter for /transactions. */
(function () {
  function create(http) {
    return {
      list:        function (params) { return http.get('/transactions' + (params ? '?' + params : '')); },
      create:      function (data)   { return http.post('/transactions', data); },
      update:      function (id, d)  { return http.patch('/transactions/' + id, d); },
      patchStatus: function (id, status, paymentDate) {
        return http.patch('/transactions/' + id + '/status', { status: status, paymentDate: paymentDate });
      },
      remove:      function (id, mode) { return http.delete('/transactions/' + id + (mode ? '?mode=' + mode : '')); },
      transfer:    function (data)   { return http.post('/transactions/transfer', data); },
      importPreview: function (file, password) {
        const fd = new FormData();
        fd.append('file', file);
        if (password) fd.append('password', password);
        return http.upload('/transactions/import/preview', fd);
      },
      importConfirm: function (data) { return http.post('/transactions/import/confirm', data); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.TransactionRepository = { create: create };
})();

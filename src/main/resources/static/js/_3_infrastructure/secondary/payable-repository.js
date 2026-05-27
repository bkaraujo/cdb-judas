/* _3_infrastructure/secondary/payable-repository.js — HTTP adapter for /operations/payables. */
(function () {
  function create(http) {
    return {
      listPayable:    function ()           { return http.get('/operations/payables?type=PAYABLE'); },
      listReceivable: function ()           { return http.get('/operations/payables?type=RECEIVABLE'); },
      confirm:        function (id, date)   { return http.put('/operations/payables/' + id + '/confirm', { paymentDate: date }); },
      cancel:         function (id)         { return http.put('/operations/payables/' + id + '/cancel', {}); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.PayableRepository = { create: create };
})();

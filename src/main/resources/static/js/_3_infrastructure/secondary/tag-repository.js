/* _3_infrastructure/secondary/tag-repository.js — HTTP adapter for /tags. */
(function () {
  function create(http) {
    return {
      list:   function ()        { return http.get('/tags'); },
      create: function (data)    { return http.post('/tags', data); },
      update: function (id, d)   { return http.patch('/tags/' + id, d); },
      remove: function (id)      { return http.delete('/tags/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.TagRepository = { create: create };
})();
